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
import org.compiere.acct.Doc_GLJournal;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAssetAddition;
import org.compiere.model.MAssetDisposed;
import org.compiere.model.MAssetReval;
import org.compiere.model.MAssetTransfer;
import org.compiere.model.MBankStatement;
import org.compiere.model.MCash;
import org.compiere.model.MDocType;
import org.compiere.model.MInventory;
import org.compiere.model.MInvoice;
import org.compiere.model.MJournal;
import org.compiere.model.MMatchInv;
import org.compiere.model.MMatchPO;
import org.compiere.model.MMovement;
import org.compiere.model.MPayment;
import org.compiere.model.MProduction;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MRequisition;
import org.compiere.model.MTable;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
public class DocFactory implements IDocFactory {

	final public String ACCT_SCHEMA_GAAP_TYPE_BXSERVICE_DATEV = "XD";

	@Override
	public Doc getDocument(MAcctSchema as, int tableId, ResultSet rs, String trxName) {
		Doc doc = null;
		if (ACCT_SCHEMA_GAAP_TYPE_BXSERVICE_DATEV.equals(as.getGAAP())) {
			if (MInvoice.Table_ID == tableId) {
				doc = new Doc_DatevInvoice(as, rs, trxName);
			} else if (MAllocationHdr.Table_ID == tableId) {
				doc = new Doc_DatevAllocationHdr(as, rs, trxName);
			} else if (MPayment.Table_ID == tableId) {
				doc = new Doc_DatevPayment(as, rs, trxName);
			} else if (MJournal.Table_ID == tableId) {
				doc = new Doc_GLJournal(as, rs, trxName);
			} else {
				MTable table = MTable.get(tableId);
				Class<?> classPO = MTable.getClass(table.getTableName());
				String docType = null;
				switch (tableId) {
				case MAssetAddition.Table_ID:
				case MAssetDisposed.Table_ID:
					docType = MDocType.DOCBASETYPE_GLDocument;					break;
				case MAssetReval.Table_ID:
				case MAssetTransfer.Table_ID:
					docType = MDocType.DOCBASETYPE_GLJournal;					break;
				case MBankStatement.Table_ID:
					docType = MDocType.DOCBASETYPE_BankStatement;				break;
				case MCash.Table_ID:
					docType = MDocType.DOCBASETYPE_CashJournal;					break;
				case MInventory.Table_ID:
					docType = MDocType.DOCBASETYPE_MaterialPhysicalInventory;	break;
				case MMatchInv.Table_ID:
					docType = MDocType.DOCBASETYPE_MatchInvoice;				break;
				case MMatchPO.Table_ID:
					docType = MDocType.DOCBASETYPE_MatchPO;						break;
				case MMovement.Table_ID:
					docType = MDocType.DOCBASETYPE_MaterialMovement;			break;
				case MProduction.Table_ID:
					docType = MDocType.DOCBASETYPE_MaterialProduction;			break;
				case MProjectIssue.Table_ID:
					docType = MDocType.DOCBASETYPE_ProjectIssue;				break;
				case MRequisition.Table_ID:
					docType = MDocType.DOCBASETYPE_PurchaseRequisition;			break;
				}
				doc = new Doc_NoAcct(as, rs, trxName, classPO, docType);
			}
		}
		return doc;
	}

}