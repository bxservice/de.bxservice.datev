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
import org.compiere.acct.Doc_Invoice;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.ProductCost;
import org.compiere.util.Util;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class Doc_DatevInvoice extends Doc_Invoice {

	/**
	 * Constructor
	 * 
	 * @param as      accounting schema
	 * @param rs      record
	 * @param trxName trx
	 */
	public Doc_DatevInvoice(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
	}

	/**
	 *  Create Facts (the accounting logic) for
	 *  ARI, ARC, ARF, API, APC.
	 *  Konto: Account_ID
	 *  Gegenkonto: UserElement1_ID
	 *  Debit/Credit Soll/Haben is determined by the AmtAcctDr/AmtAcctCr
	 *  <pre>
	 *  ARI, ARF
	 *      Receivables     DR
	 *      Revenue/Charge          CR
	 *
	 *  ARC
	 *      Receivables             CR
	 *      Revenue/Charge  DR
	 *
	 *  API
	 *      Payables                CR
	 *      Expense/Charge  DR
	 *
	 *  APC
	 *      Payables        DR
	 *      Expense/Charge          CR
	 *  </pre>
	 *  @param as accounting schema
	 *  @return Fact
	 */
	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		//
		ArrayList<Fact> facts = new ArrayList<Fact>();
		// create Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);

		boolean isSOTrx = true;
		boolean isCreditNote = false;
		if (DOCTYPE_ARInvoice.equals(getDocumentType()) || DOCTYPE_ARProForma.equals(getDocumentType())) {
			// ** ARI, ARF - Sales Invoice
			isSOTrx = true;
			isCreditNote = false;
		} else if (DOCTYPE_ARCredit.equals(getDocumentType())) {
			// ARC - Credit Note Sales
			isSOTrx = true;
			isCreditNote = true;
		} else if (DOCTYPE_APInvoice.equals(getDocumentType())) {
			// ** API - Vendor Invoice
			isSOTrx = false;
			isCreditNote = false;
		} else if (DOCTYPE_APCredit.equals(getDocumentType())) {
			// APC - Vendor Credit Note
			isSOTrx = false;
			isCreditNote = true;
		} else {
			p_Error = "DocumentType unknown: " + getDocumentType();
			log.log(Level.SEVERE, p_Error);
			fact = null;
		}

		int currency_ID = getC_Currency_ID();

		int receivables_ID = 0;
		int payables_ID = 0;
		if (isSOTrx)
			receivables_ID = getValidCombination_ID(Doc.ACCTTYPE_C_Receivable, as);
		else
			payables_ID = getValidCombination_ID(Doc.ACCTTYPE_V_Liability, as);

		for (DocLine docLine : p_lines) {

			// Amount
			BigDecimal amt = docLine.getAmtSource();
			if (amt.signum() == 0)
				continue;

			// Accounts
			MAccount revenue = null;
			MAccount expense = null;
			MAccount charge = null;
			if (docLine.getM_Product_ID() > 0) {
				if (isSOTrx)
					revenue = docLine.getAccount(ProductCost.ACCTTYPE_P_Revenue, as);
				else
					expense = docLine.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
			} else {
				charge = docLine.getChargeAccount(as, null);
			}

			MAccount drAccount = null;
			MAccount crAccount = null;
			if (isSOTrx) {
				if (isCreditNote) {
					drAccount = (revenue != null ? revenue : charge);
					crAccount = MAccount.get(getCtx(), receivables_ID);
				} else {
					drAccount = MAccount.get(getCtx(), receivables_ID);
					crAccount = (revenue != null ? revenue : charge);
				}
			} else {
				if (isCreditNote) {
					drAccount = MAccount.get(getCtx(), payables_ID);
					crAccount = (expense != null ? expense : charge);
				} else {
					drAccount = (expense != null ? expense : charge);
					crAccount = MAccount.get(getCtx(), payables_ID);
				}
			}

			if (drAccount != null && crAccount != null) {
				createLine(fact, docLine, drAccount, currency_ID, amt, null, null); // create debit line
				createLine(fact, docLine, crAccount, currency_ID, null, amt, null); // create credit line
			} else {
				p_Error = "Could not find accounts for posting";
				log.log(Level.SEVERE, p_Error);
				fact = null;
			}

		}	
		//
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
	 */
	private FactLine createLine(Fact fact, DocLine docLine, MAccount account, int currency_ID, BigDecimal dr, BigDecimal cr, String description) {
		FactLine factLine = fact.createLine(docLine, account, currency_ID, dr, cr);
		if (factLine != null) {
			factLine.setQty(docLine.getQty());
			factLine.setC_Tax_ID(docLine.getC_Tax_ID());
			factLine.setLocationFromOrg(factLine.getAD_Org_ID(), true); // from Loc
			factLine.setLocationFromBPartner(getC_BPartner_Location_ID(), false); // to Loc
			if (Util.isEmpty(description))
				factLine.setDescription(description);
		}
		return factLine;
	}

}
