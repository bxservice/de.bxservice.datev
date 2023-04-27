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
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.util.DB;

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
	 *  Record sales invoice
	 *  	PayDiscount_Exp_Acct (discount and write-off)
	 *  		C_Receivable_Acct
	 *  
	 *  Record purchase invoice
	 *  		PayDiscount_Rev_Acct (discount and write-off)
	 *  	V_Liability_Acct
	 *  
	 *  Record sales payment
	 *  	B_Asset_Acct
	 *  
	 *  Record purchase payment
			 *  B_Asset_Acct
	 *  
	 *  Record charge
	 *  	CH_Expense_Acct (where it goes depends on the sign)
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
			setC_BPartner_ID(line.getC_BPartner_ID());
			int currency_ID = getC_Currency_ID();

			MInvoice invoice = null;
			int taxId = -1;
			if (line.getC_Invoice_ID() > 0) {
				// Invoice posting
				invoice = new MInvoice (getCtx(), line.getC_Invoice_ID(), getTrxName());
				// TODO: multi-tax invoices are not supported
				// get first tax from the invoice
				taxId = DB.getSQLValue(getTrxName(), "SELECT MIN(C_Tax_ID) FROM C_InvoiceLine WHERE C_Invoice_ID=? AND QtyInvoiced!=0 AND IsActive='Y'", invoice.getC_Invoice_ID());
				MAccount bpAccount = null;
				if (invoice.isSOTrx())
					bpAccount = getAccount(Doc.ACCTTYPE_C_Receivable, as);
				else
					bpAccount = getAccount(Doc.ACCTTYPE_V_Liability, as);
				BigDecimal amt = line.getAmtSource().add(line.getDiscountAmt()).add(line.getWriteOffAmt());
				FactLine fl = DatevHelper.createLine(fact, this, lineraw, bpAccount, currency_ID, amt.negate(), null, taxId, DatevHelper.ELEMENT_VALUE_AllocationBPRevenueLiability); // create BP AR/AP line
				if (fl != null) {
					MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), invoice.getC_BPartner_Location_ID(), getTrxName());
					fl.setUser1_ID(DatevHelper.getDATEVRegionFromLocation(bpl.getC_Location_ID(), invoice.getDateAcct()));
					fl.setUserElement1_ID(invoice.getC_Invoice_ID());
				}
			}
			if (line.getC_Payment_ID() > 0) {
				// Payment posting
				MPayment payment = new MPayment (getCtx(), line.getC_Payment_ID(), getTrxName());
				setC_BankAccount_ID(payment.getC_BankAccount_ID());
				MAccount acct_bank = getAccount(ACCTTYPE_BankAsset, as);
				BigDecimal amt = line.getAmtSource();
				FactLine fl = DatevHelper.createLine(fact, this, lineraw, acct_bank, currency_ID, amt, null, taxId, DatevHelper.ELEMENT_VALUE_AllocationPaymentBank); // create payment line
				if (fl != null && invoice != null) {
					MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), invoice.getC_BPartner_Location_ID(), getTrxName());
					fl.setUser1_ID(DatevHelper.getDATEVRegionFromLocation(bpl.getC_Location_ID(), invoice.getDateAcct()));
					fl.setUserElement1_ID(invoice.getC_Invoice_ID());
				}
			}
			if (line.getDiscountAmt().signum() != 0 || line.getWriteOffAmt().signum() != 0) {
				// Discount/WriteOff posting
				BigDecimal discountPlusWriteOff = line.getDiscountAmt().add(line.getWriteOffAmt());
				MAccount discAcct = getAccount(Doc.ACCTTYPE_DiscountExp, as);
				FactLine fl = DatevHelper.createLine(fact, this, lineraw, discAcct, currency_ID, discountPlusWriteOff, null, taxId, DatevHelper.ELEMENT_VALUE_AllocationDiscountWriteOff); // create discount line
				if (fl != null && invoice != null) {
					MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), invoice.getC_BPartner_Location_ID(), getTrxName());
					fl.setUser1_ID(DatevHelper.getDATEVRegionFromLocation(bpl.getC_Location_ID(), invoice.getDateAcct()));
					fl.setUserElement1_ID(invoice.getC_Invoice_ID());
				}
			}
			if (line.getC_Charge_ID() > 0) {
				// Charge posting
				MAccount chargeAcct = line.getChargeAccount(as, line.getAmtSource());
				BigDecimal amt = line.getAmtSource();
				DatevHelper.createLine(fact, this, lineraw, chargeAcct, currency_ID, amt, null, -1, DatevHelper.ELEMENT_VALUE_AllocationCharge); // create charge line
			}

		}	//	for all lines

		facts.add(fact);
		return facts;

	}

}