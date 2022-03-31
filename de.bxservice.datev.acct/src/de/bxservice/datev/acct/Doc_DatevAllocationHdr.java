/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Carlos Ruiz - globalqss - bxservice                               *
 **********************************************************************/

package de.bxservice.datev.acct;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.acct.Doc;
import org.compiere.acct.DocLine;
import org.compiere.acct.DocLine_Allocation;
import org.compiere.acct.Doc_AllocationHdr;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class Doc_DatevAllocationHdr extends Doc_AllocationHdr {

	/**
	 * Constructor
	 * 
	 * @param as      accounting schema
	 * @param rs      record
	 * @param trxName trx
	 */
	public Doc_DatevAllocationHdr(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
	}

	/**
	 *  Create Facts (the accounting logic) for
	 *  CMA.
	 *  <pre>
	 *  AR_Invoice_Payment
	 *      Receivables     DR     (discount and writeoff in CR)
	 *      Bank Account            CR
	 *  AP_Invoice_Payment
	 *      Payables        DR     (discount and writeoff in CR)
	 *      Bank Account            CR
	 *  </pre>
	 * @param as account schema
	 * @return Facts empty array - no accounting
	 */
	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		//
		ArrayList<Fact> facts = new ArrayList<Fact>();
		// create Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);

		for (DocLine lineraw : p_lines) {
			DocLine_Allocation line = null;
			if (!(lineraw instanceof DocLine_Allocation)) {
				p_Error = "No DocLine_Allocation";
				log.log(Level.SEVERE, p_Error);
				return null;
			}

			line = (DocLine_Allocation) lineraw;
			if (line.getC_Payment_ID() <= 0 || line.getC_Invoice_ID() <= 0)
				continue;

			setC_BPartner_ID(line.getC_BPartner_ID());

			//	Receivables/Liability Amt
			BigDecimal discountPlusWriteOff = line.getDiscountAmt().add(line.getWriteOffAmt());
			BigDecimal allocationSource = line.getAmtSource().add(discountPlusWriteOff);

			MPayment payment = new MPayment (getCtx(), line.getC_Payment_ID(), getTrxName());
			MInvoice invoice = new MInvoice (getCtx(), line.getC_Invoice_ID(), getTrxName());

			setC_BankAccount_ID(payment.getC_BankAccount_ID());
			MAccount acct_bank = getAccount(ACCTTYPE_BankAsset, as);

			// get first tax from the invoice
			int taxId = DB.getSQLValue(getTrxName(), "SELECT MIN(C_Tax_ID) FROM C_InvoiceLine WHERE C_Invoice_ID=? AND QtyInvoiced!=0 AND IsActive='Y'", invoice.getC_Invoice_ID());

			if (invoice.isSOTrx()) {
				//	Sales Invoice
				MAccount acct_receivable = getAccount(Doc.ACCTTYPE_C_Receivable, as);
				createLine(fact, line, acct_receivable, getC_Currency_ID(), allocationSource, discountPlusWriteOff, null, taxId);
				createLine(fact, line, acct_bank, getC_Currency_ID(), null, line.getAmtSource(), null, taxId);
			} else {
				//	Purchase Invoice
				MAccount acct_liability = getAccount(Doc.ACCTTYPE_V_Liability, as);
				createLine(fact, line, acct_liability, getC_Currency_ID(), allocationSource.negate(), discountPlusWriteOff.negate(), null, taxId);
				createLine(fact, line, acct_bank, getC_Currency_ID(), null, line.getAmtSource().negate(), null, taxId);
			}
		}	//	for all lines

		facts.add(fact);
		return facts;

	}

	/**
	 * Create a line in the fact with the information provided
	 * @param fact
	 * @param docLine
	 * @param drAccount
	 * @param currency_ID
	 * @param dr
	 * @param cr
	 * @param description
 	 * @param taxID
	 */
	private FactLine createLine(Fact fact, DocLine docLine, MAccount account, int currency_ID, BigDecimal dr, BigDecimal cr, String description, int taxID) {
		FactLine factLine = fact.createLine(docLine, account, currency_ID, dr, cr);
		if (factLine != null) {
			factLine.setQty(Env.ZERO);
			factLine.setC_Tax_ID(taxID);
			factLine.setLocationFromOrg(factLine.getAD_Org_ID(), true); // from Loc
			factLine.setLocationFromBPartner(getC_BPartner_Location_ID(), false); // to Loc
			if (Util.isEmpty(description))
				factLine.setDescription(description);
		}
		return factLine;
	}

}