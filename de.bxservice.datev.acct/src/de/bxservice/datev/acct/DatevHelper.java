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
 * - Carlos Ruiz - BX Service                                          *
 **********************************************************************/
package de.bxservice.datev.acct;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.acct.Doc;
import org.compiere.acct.DocLine;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MOrder;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

public class DatevHelper {

	/**	Static Log						*/
	protected static final CLogger	s_log = CLogger.getCLogger(DatevHelper.class);

	private static final String CODE_COUNTRY_GROUP_EU = "EU";
	
	private static final String CODE_ELEMENT_DATEV_Region = "DATEV Region";
	private static final String CODE_ELEMENT_DATEV_Record_Type = "DATEV Record Type";

	private static final String CODE_ELEMENT_VALUE_GERMANY = "G";
	private static final String CODE_ELEMENT_VALUE_INTRAEU = "EU";
	private static final String CODE_ELEMENT_VALUE_EXTRAEU = "W";
	/* DATEV Type */
	/** Allocation BP Revenue/Liability = AB */
	private static final String CODE_ELEMENT_VALUE_AllocationBPRevenueLiability = "AB";
	/** Allocation Charge = AC */
	private static final String CODE_ELEMENT_VALUE_AllocationCharge = "AC";
	/** Allocation Discount/WriteOff = AD */
	private static final String CODE_ELEMENT_VALUE_AllocationDiscountWriteOff = "AD";
	/** Allocation Payment Bank = AP */
	private static final String CODE_ELEMENT_VALUE_AllocationPaymentBank = "AP";
	/** Invoice BP Receivable/Liability = IB */
	private static final String CODE_ELEMENT_VALUE_InvoiceBPReceivableLiability = "IB";
	/** Invoice Product/Charge Revenue/Expense = IP */
	private static final String CODE_ELEMENT_VALUE_InvoiceProductChargeRevenueExpense = "IP";
	/** Invoice Tax Due/Credit = IT */
	private static final String CODE_ELEMENT_VALUE_InvoiceTaxDueCredit = "IT";
	/** Payment Charge = PC */
	private static final String CODE_ELEMENT_VALUE_PaymentCharge = "PC";
	/** Payment Bank = PP */
	private static final String CODE_ELEMENT_VALUE_PaymentBank = "PP";

	private static final int COUNTRY_GERMANY = 101;
	private static int COUNTRY_GROUP_EU;
	private static int ELEMENT_VALUE_GERMANY;
	private static int ELEMENT_VALUE_INTRAEU;
	private static int ELEMENT_VALUE_EXTRAEU;
	public static int ELEMENT_VALUE_AllocationBPRevenueLiability;
	public static int ELEMENT_VALUE_AllocationCharge;
	public static int ELEMENT_VALUE_AllocationDiscountWriteOff;
	public static int ELEMENT_VALUE_AllocationPaymentBank;
	public static int ELEMENT_VALUE_InvoiceBPReceivableLiability;
	public static int ELEMENT_VALUE_InvoiceProductChargeRevenueExpense;
	public static int ELEMENT_VALUE_InvoiceTaxDueCredit;
	public static int ELEMENT_VALUE_PaymentBank;
	public static int ELEMENT_VALUE_PaymentCharge;

	static {
		COUNTRY_GROUP_EU = getCountryGroup(CODE_COUNTRY_GROUP_EU);
		ELEMENT_VALUE_GERMANY = getElementValue(CODE_ELEMENT_DATEV_Region, CODE_ELEMENT_VALUE_GERMANY);
		ELEMENT_VALUE_INTRAEU = getElementValue(CODE_ELEMENT_DATEV_Region, CODE_ELEMENT_VALUE_INTRAEU);
		ELEMENT_VALUE_EXTRAEU = getElementValue(CODE_ELEMENT_DATEV_Region, CODE_ELEMENT_VALUE_EXTRAEU);
		ELEMENT_VALUE_AllocationBPRevenueLiability = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_AllocationBPRevenueLiability);
		ELEMENT_VALUE_AllocationCharge = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_AllocationCharge);
		ELEMENT_VALUE_AllocationDiscountWriteOff = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_AllocationDiscountWriteOff);
		ELEMENT_VALUE_AllocationPaymentBank = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_AllocationPaymentBank);
		ELEMENT_VALUE_InvoiceBPReceivableLiability = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_InvoiceBPReceivableLiability);
		ELEMENT_VALUE_InvoiceProductChargeRevenueExpense = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_InvoiceProductChargeRevenueExpense);
		ELEMENT_VALUE_InvoiceTaxDueCredit = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_InvoiceTaxDueCredit);
		ELEMENT_VALUE_PaymentBank = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_PaymentBank);
		ELEMENT_VALUE_PaymentCharge = getElementValue(CODE_ELEMENT_DATEV_Record_Type, CODE_ELEMENT_VALUE_PaymentCharge);
	}

	private static int getCountryGroup(String codeCountryGroup) {
		final String sql = "SELECT C_CountryGroup_ID FROM C_CountryGroup WHERE AD_Client_ID IN (0,?) AND Value=? AND IsActive='Y'";
		int cg = DB.getSQLValueEx(null, sql, Env.getAD_Client_ID(Env.getCtx()), codeCountryGroup);
		if (cg < 0) {
			String msg = "Country Group " + codeCountryGroup + " not configured";
			s_log.severe(msg);
			throw new AdempiereException(msg);
		}
		return cg;
	}

	private static int getElementValue(String codeElement, String codeElementValue) {
		final String sql = "SELECT C_ElementValue_ID "
				+ "FROM C_ElementValue ev "
				+ "JOIN C_Element e ON (ev.C_Element_ID=e.C_Element_ID) "
				+ "WHERE ev.AD_Client_ID IN (0,?) "
				+ "AND e.Name=? "
				+ "AND ev.Value=? "
				+ "AND e.IsActive='Y' "
				+ "AND ev.IsActive='Y'";
		int ev = DB.getSQLValueEx(null, sql, Env.getAD_Client_ID(Env.getCtx()), codeElement, codeElementValue);
		if (ev < 0) {
			String msg = "Element Value " + codeElementValue + " not configured in Element " + codeElement;
			s_log.severe(msg);
			throw new AdempiereException(msg);
		}
		return ev;
	}

	public static int getDATEVRegionFromLocation(int locId, Timestamp dateDoc) {
		MLocation loc = MLocation.get(locId);
		if (loc != null) {
			if (loc.getC_Country_ID() == COUNTRY_GERMANY)
				return ELEMENT_VALUE_GERMANY;
			if (countryGroupContains(COUNTRY_GROUP_EU, loc.getC_Country_ID(), dateDoc))
				return ELEMENT_VALUE_INTRAEU;
		}
		return ELEMENT_VALUE_EXTRAEU;
	}

	private static boolean countryGroupContains(int countryGroupID, int countryID, Timestamp dateDoc) {
		final String sql = ""
				+ "SELECT COUNT(*) "
				+ "FROM   C_CountryGroup cg "
				+ "JOIN C_CountryGroupCountry cgc ON (cg.C_CountryGroup_ID=cgc.C_CountryGroup_ID) "
				+ "WHERE  cgc.C_Country_ID = ? "
				+ "       AND cg.C_CountryGroup_ID = ? "
				+ "       AND cgc.IsActive = 'Y' "
				+ "       AND (cgc.DateTo IS NULL OR cgc.DateTo <= ?)";
		int cnt = DB.getSQLValueEx(null, sql, countryID, countryGroupID, dateDoc);
		return cnt > 0;
	}

	/**
	 * Create a line in the fact with the information provided
	 * @param fact
	 * @param docLine
	 * @param drAccount
	 * @param currency_ID
	 * @param amt  Amount - positive post as DR, negative post as CR
	 * @param description
	 * @param bxDatevType 
	 */
	public static FactLine createLine(Fact fact, Doc doc, DocLine docLine, MAccount account, int currency_ID, BigDecimal amt, String description, int taxID, int bxDatevType) {
		if (amt.signum() == 0)
			return null;
		FactLine factLine = fact.createLine(docLine, account, currency_ID, amt, null);
		if (factLine != null) {
			if (bxDatevType == ELEMENT_VALUE_InvoiceProductChargeRevenueExpense && docLine != null) // set the quantity just for invoice product postings
				factLine.setQty(docLine.getQty());
			else
				factLine.setQty(Env.ZERO);
			factLine.setC_Tax_ID(taxID);
			factLine.setLocationFromOrg(factLine.getAD_Org_ID(), doc.isSOTrx());

			// for drop ship orders set the location based on the drop ship location of the order
			int cbpl = doc.getC_BPartner_Location_ID();
			if (   bxDatevType == ELEMENT_VALUE_InvoiceBPReceivableLiability
				|| bxDatevType == ELEMENT_VALUE_InvoiceProductChargeRevenueExpense
				|| bxDatevType == ELEMENT_VALUE_InvoiceTaxDueCredit) {
				MInvoice invoice = (MInvoice) doc.getPO();
				MOrder order = invoice.getOriginalOrder();
				if (order != null && order.isDropShip() && order.getDropShip_Location_ID() > 0) {
					// set the BP Location according to the drop ship location
					cbpl = order.getDropShip_Location_ID();
				}
			}

			factLine.setLocationFromBPartner(cbpl, !doc.isSOTrx());
			factLine.setUser1_ID(DatevHelper.getDATEVRegionFromLocation( (doc.isSOTrx() ? factLine.getC_LocTo_ID() : factLine.getC_LocFrom_ID()), doc.getDateAcct()));
			factLine.setUser2_ID(bxDatevType);
			if (!Util.isEmpty(description))
				factLine.setDescription(description);
		}
		return factLine;
	}

}
