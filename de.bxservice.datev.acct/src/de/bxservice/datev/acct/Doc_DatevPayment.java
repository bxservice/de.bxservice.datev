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

import org.compiere.acct.Doc;
import org.compiere.acct.Doc_Payment;
import org.compiere.acct.Fact;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MCharge;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class Doc_DatevPayment extends Doc_Payment {

	/**
	 * Constructor
	 * 
	 * @param as      accounting schema
	 * @param rs      record
	 * @param trxName trx
	 */
	public Doc_DatevPayment(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
	}

	/**
	 *  Create Facts (the accounting logic) for
	 *  CMA.
	 *  <pre>
	 *  Post payment vs charges
	 *  </pre>
	 * @param as account schema
	 * @return Facts empty array - no accounting
	 */
	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		//
		ArrayList<Fact> facts = new ArrayList<Fact>();
		//  create Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);
		if (getC_Charge_ID() != 0) {
			int currency_ID = getC_Currency_ID();
			MAccount chargeAcct = MCharge.getAccount(getC_Charge_ID(), as);
			MAccount bankAcct = getAccount(Doc.ACCTTYPE_BankAsset, as);
			BigDecimal amt = getAmount();
			if (getDocumentType().equals(DOCTYPE_APPayment))
				amt = amt.negate();
			DatevHelper.createLine(fact, this, null, bankAcct, currency_ID, amt, null, -1, DatevHelper.ELEMENT_VALUE_PaymentBank); // create bank line
			DatevHelper.createLine(fact, this, null, chargeAcct, currency_ID, amt.negate(), null, -1, DatevHelper.ELEMENT_VALUE_PaymentCharge); // create charge line
		}
		facts.add(fact);
		return facts;
	}

}