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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.acct.Doc;
import org.compiere.acct.DocLine;
import org.compiere.acct.Doc_Invoice;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MClientInfo;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocation;
import org.compiere.model.ProductCost;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class Doc_DatevInvoice extends Doc_Invoice {

	private static final int COUNTRY_GERMANY = 101;
	private static final String COUNTRY_GROUP_EU = "e2d931e1-cb48-488a-bbb6-b6af006d9388";
	private static final String ACCT_PO_GERMANY = "3400";
	private static final String ACCT_PO_INTRA_EU = "3425";
	private static final String ACCT_PO_EXTRA_EU = "3200";
	private static final String ACCT_SO_GERMANY = "8400";
	private static final String ACCT_SO_INTRA_EU = "8125";
	private static final String ACCT_SO_EXTRA_EU = "8120";

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

			MInvoiceLine il = (MInvoiceLine) docLine.getPO();
			// docLine.getAmtSource() is without tax, we need it including tax
			// WARNING: This must work just for taxes that are non-summary
			BigDecimal amt = il.getLineTotalAmt();
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
			changeAccountBasedOnCountryGroup(factLine, docLine);
		}
		return factLine;
	}

	private void changeAccountBasedOnCountryGroup(FactLine factLine, DocLine docLine) {
		// Change accounts
		//   PURCHASING accounting / Export:
		//     Germany - 3400
		//     Intra EU - 3425
		//     Extra EU - 3200
		//   SALES accounting / Import:
		//     Germany - 8400
		//     Intra EU - 8125
		//     Extra EU - 8120
		MInvoiceLine il = (MInvoiceLine) docLine.getPO();
		MInvoice i = il.getParent();
		MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), i.getC_BPartner_Location_ID(), getTrxName());
		MLocation loc = MLocation.get(bpl.getC_Location_ID());
		int countryId = loc.getC_Country_ID();
		if (countryId == COUNTRY_GERMANY)
			return;
		boolean isEU = countryGroupContains(COUNTRY_GROUP_EU, countryId, getDateDoc());
		if (i.isSOTrx()) {
			// Sales
			int germanyId = getAcctId(ACCT_SO_GERMANY);
			if (factLine.getAccount_ID() == germanyId) {
				if (isEU) {
					int intraEUId = getAcctId(ACCT_SO_INTRA_EU);
					factLine.setAccount_ID(intraEUId);
				} else {
					int extraEUId = getAcctId(ACCT_SO_EXTRA_EU);
					factLine.setAccount_ID(extraEUId);
				}
			}
		} else {
			// Purchase
			int germanyId = getAcctId(ACCT_PO_GERMANY);
			if (factLine.getAccount_ID() == germanyId) {
				if (isEU) {
					int intraEUId = getAcctId(ACCT_PO_INTRA_EU);
					factLine.setAccount_ID(intraEUId);
				} else {
					int extraEUId = getAcctId(ACCT_PO_EXTRA_EU);
					factLine.setAccount_ID(extraEUId);
				}
			}
		}
	}

	private int getAcctId(String acctValue) {
		MClientInfo clientInfo = MClientInfo.get(getCtx());
		MAcctSchema primary = clientInfo.getMAcctSchema1();
		MAcctSchemaElement ele = primary.getAcctSchemaElement(MAcctSchemaElement.ELEMENTTYPE_Account);
		final String sql = ""
				+ "SELECT C_ElementValue_ID "
				+ "FROM C_ElementValue "
				+ "WHERE Value = ? AND IsActive = 'Y' AND C_Element_ID = ?";
		int acctId = DB.getSQLValueEx(getTrxName(), sql, acctValue, ele.getC_Element_ID());
		if (acctId <= 0)
			throw new AdempiereException("Could not find Account (C_ElementValue) with code " + acctValue);
		return acctId;
	}

	private static boolean countryGroupContains(String countryGroupUU, int countryID, Timestamp dateDoc) {
		final String sql = ""
				+ "SELECT COUNT(*) "
				+ "FROM   C_CountryGroup cg "
				+ "JOIN C_CountryGroupCountry cgc ON (cg.C_CountryGroup_ID=cgc.C_CountryGroup_ID) "
				+ "WHERE  cgc.C_Country_ID = ? "
				+ "       AND C_CountryGroup_UU = ? "
				+ "       AND cgc.IsActive = 'Y' "
				+ "       AND (cgc.DateTo IS NULL OR cgc.DateTo <= ?)";
		int cnt = DB.getSQLValue(null, sql, countryID, countryGroupUU, dateDoc);
		return cnt > 0;
	}

}
