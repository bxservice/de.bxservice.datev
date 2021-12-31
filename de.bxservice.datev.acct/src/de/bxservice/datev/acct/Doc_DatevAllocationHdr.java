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

import java.sql.ResultSet;
import java.util.ArrayList;

import org.compiere.acct.Doc_AllocationHdr;
import org.compiere.acct.Fact;
import org.compiere.model.MAcctSchema;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class Doc_DatevAllocationHdr extends Doc_AllocationHdr {

	/**
	 * Constructor
	 * @param as      accounting schema
	 * @param rs      record
	 * @param trxName trx
	 */
	public Doc_DatevAllocationHdr(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
	}

	/**
	 * Create Facts (the accounting logic)
	 * 
	 * @param as account schema
	 * @return Facts empty array - no accounting
	 */
	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		// TODO:
		ArrayList<Fact> facts = new ArrayList<Fact>();
		return facts;
	}

}
