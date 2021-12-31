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

import org.compiere.acct.Doc_GLJournal;
import org.compiere.acct.Fact;
import org.compiere.model.MAcctSchema;
import org.compiere.util.Env;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class Doc_NoAcct extends Doc_GLJournal {

	/**
	 * Constructor
	 * @param as      accounting schema
	 * @param rs      record
	 * @param trxName trx
	 */
	public Doc_NoAcct(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
	}

	/**
	 * Load Specific Document Details
	 * 
	 * @return null - no accounting
	 */
	@Override
	protected String loadDocumentDetails() {
		return null;
	}

	/**
	 * Get Source Currency Balance - subtracts line amounts from total - no rounding
	 * 
	 * @return ZERO - no accounting
	 */
	@Override
	public BigDecimal getBalance() {
		return Env.ZERO;
	}

	/**
	 * Create Facts (the accounting logic)
	 * 
	 * @param as account schema
	 * @return Facts empty array - no accounting
	 */
	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		ArrayList<Fact> facts = new ArrayList<Fact>();
		return facts;
	}

}
