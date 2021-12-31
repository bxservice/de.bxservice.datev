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

import org.adempiere.base.IDocFactory;
import org.compiere.acct.Doc;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MInvoice;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class DocFactory implements IDocFactory {

	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, ResultSet rs, String trxName) {
		Doc doc = null;
		if ("XD".equals(as.getGAAP())) {
			if (MInvoice.Table_ID == AD_Table_ID) {
				doc = new Doc_DatevInvoice(as, rs, trxName);
			} else if (MAllocationHdr.Table_ID == AD_Table_ID) {
				doc = new Doc_DatevAllocationHdr(as, rs, trxName);
			} else {
				doc = new Doc_NoAcct(as, rs, trxName);
			}
		}
		return doc;
	}

}