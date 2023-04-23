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
import org.compiere.acct.DocTax;
import org.compiere.acct.Doc_Invoice;
import org.compiere.acct.Fact;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MTax;
import org.compiere.model.ProductCost;

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
	 *  <pre>
	 *  Each line generates three postings:
	 *  ARI, ARF
	 *      Receivables     DR
	 *          Charge|Revenue      CR
	 *          TaxDue              CR
	 *
	 *  ARC
	 *          Receivables         CR
	 *      Charge|Revenue  DR
	 *      TaxDue          DR
	 *
	 *  API
	 *          Payables            CR
	 *      Charge|Expense  DR
	 *      TaxCredit       DR
	 *
	 *  APC
	 *      Payables        DR
	 *          Charge|Expense      CR
	 *          TaxCredit           CR
	 *  </pre>
	 *  @param as accounting schema
	 *  @return Fact
	 */
	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		/* TODO: Validate - multi-tax invoiced not supported */
		/* TODO: Validate - summary taxes not supported */
		/* TODO: Validate - lines without charge or product not supported */
		//
		ArrayList<Fact> facts = new ArrayList<Fact>();
		// create Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);

		boolean isSOTrx = isSOTrx();
		boolean isCreditNote = getDocumentType().substring(2).equals("C");

		int currency_ID = getC_Currency_ID();

		int receivables_ID = 0;
		int payables_ID = 0;
		if (isSOTrx)
			receivables_ID = getValidCombination_ID(Doc.ACCTTYPE_C_Receivable, as);
		else
			payables_ID = getValidCombination_ID(Doc.ACCTTYPE_V_Liability, as);

		for (DocLine docLine : p_lines) {

			MInvoiceLine il = (MInvoiceLine) docLine.getPO();
			// docLine.getAmtSource() is without tax, we need it including tax
			// WARNING: This must work just for taxes that are non-summary
			BigDecimal amtTotal = il.getLineTotalAmt();
			BigDecimal amtNet = docLine.getAmtSource();
			BigDecimal amtTax = amtTotal.subtract(amtNet);
			MTax tax = MTax.get(il.getC_Tax_ID());
			DocTax docTax = new DocTax(tax.getC_Tax_ID(), tax.getName(), tax.getRate(), amtNet, amtTax, tax.isSalesTax());

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

			MAccount bpAccount = null;
			MAccount prAccount = null;
			MAccount taxAccount = null;
			amtNet = amtNet.negate();
			amtTax = amtTax.negate();
			if (isSOTrx) {
				taxAccount = docTax.getAccount(DocTax.ACCTTYPE_TaxDue, as);
				bpAccount = MAccount.get(getCtx(), receivables_ID);
				prAccount = (revenue != null ? revenue : charge);
			} else {
				taxAccount = docTax.getAccount(DocTax.ACCTTYPE_TaxCredit, as);
				bpAccount = MAccount.get(getCtx(), payables_ID);
				prAccount = (expense != null ? expense : charge);
			}
			if (isCreditNote) {
				amtTotal = amtTotal.negate();
				amtNet = amtNet.negate();
				amtTax = amtTax.negate();
			}

			if (bpAccount != null && prAccount != null) {
				DatevHelper.createLine(fact, this, docLine, bpAccount, currency_ID, amtTotal, null, docLine.getC_Tax_ID(), DatevHelper.ELEMENT_VALUE_InvoiceBPReceivableLiability); // create BP AR/AP line
				DatevHelper.createLine(fact, this, docLine, prAccount, currency_ID, amtNet, null, docLine.getC_Tax_ID(), DatevHelper.ELEMENT_VALUE_InvoiceProductChargeRevenueExpense); // create Product Rev/Exp line
				DatevHelper.createLine(fact, this, docLine, taxAccount, currency_ID, amtTax, null, docLine.getC_Tax_ID(), DatevHelper.ELEMENT_VALUE_InvoiceTaxDueCredit); // create tax line
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

}
