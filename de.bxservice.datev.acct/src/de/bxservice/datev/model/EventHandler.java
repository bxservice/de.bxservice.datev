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

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
package de.bxservice.datev.model;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalLine;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Msg;
import org.osgi.service.event.Event;

/**
 * Event Handler for DATEv plugin
 */
public class EventHandler extends AbstractEventHandler {
	/** Logger */
	private static CLogger log = CLogger.getCLogger(EventHandler.class);

	/**
	 * Initialize Validation
	 */
	@Override
	protected void initialize() {
		log.warning("");

		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MJournal.Table_Name);
	} // initialize

	/**
	 * Handle the events
	 * 
	 * @param event
	 * @exception Exception if the recipient wishes the change to be not accept.
	 */
	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		PO po = getPO(event);
		log.info(po + " Type: " + type);
		if (po instanceof MJournal && type.equals(IEventTopics.DOC_BEFORE_COMPLETE)) {
			journalValidations((MJournal) po);
		}

	} // doHandleEvent

	/**
	 * Add validations to allow completing GL Journal
	 * - Journal must have two lines (only) with the same amount, once as debit and once as credit, and the same tax rate
	 * - Account is mandatory
	 * throws RunTimeException when an error is found
	 * @param po
	 */
	private void journalValidations(MJournal journal) {
		MAcctSchema sch = MAcctSchema.get(journal.getC_AcctSchema_ID());
		// these validations are just for DATEv schema
		if (! "XD".equals(sch.getGAAP()))
			return;

		// just journals with two lines accepted
		MJournalLine[] jLines = journal.getLines(false);
		if (jLines.length != 2)
			throw new AdempiereException(Msg.getMsg(journal.getCtx(), "BXS_DATEV_JournalMustHave2Lines"));

		MJournalLine debitLine = null;
		MJournalLine creditLine = null;
		for (MJournalLine jLine : jLines) {
			if (jLine.getAmtSourceDr().signum() > 0 && jLine.getAmtSourceCr().signum() == 0)
				debitLine = jLine;
			if (jLine.getAmtSourceDr().signum() == 0 && jLine.getAmtSourceCr().signum() > 0)
				creditLine = jLine;
		}

		// the journal must have one debit line and one credit line
		if (debitLine == null || creditLine == null)
			throw new AdempiereException(Msg.getMsg(journal.getCtx(), "BXS_DATEV_JournalMustHave2Lines"));

		// debit and credit line must have the same amount
		if (debitLine.getAmtSourceDr().compareTo(creditLine.getAmtSourceCr()) != 0)
			throw new AdempiereException(Msg.getMsg(journal.getCtx(), "BXS_DATEV_JournalDebitAndCreditMustBeEqual"));

		// account is mandatory
		if (debitLine.getAccount_ID() <= 0 || creditLine.getAccount_ID() <= 0)
			throw new AdempiereException(Msg.getMsg(journal.getCtx(), "BXS_DATEV_JournalAccountMandatory"));

		// dimensions used must be the same
		if (   (debitLine.getAD_Org_ID()             != creditLine.getAD_Org_ID())
			|| (debitLine.getAD_OrgTrx_ID()          != creditLine.getAD_OrgTrx_ID())
			|| (debitLine.getC_Activity_ID()         != creditLine.getC_Activity_ID())
			|| (debitLine.getC_BPartner_ID()         != creditLine.getC_BPartner_ID())
			|| (debitLine.getC_Campaign_ID()         != creditLine.getC_Campaign_ID())
			|| (debitLine.getC_ConversionType_ID()   != creditLine.getC_ConversionType_ID())
			|| (debitLine.getC_Currency_ID()         != creditLine.getC_Currency_ID())
			|| (debitLine.getC_LocFrom_ID()          != creditLine.getC_LocFrom_ID())
			|| (debitLine.getC_LocTo_ID()            != creditLine.getC_LocTo_ID())
			|| (debitLine.getC_Project_ID()          != creditLine.getC_Project_ID())
			|| (debitLine.getC_SalesRegion_ID()      != creditLine.getC_SalesRegion_ID())
			|| (debitLine.getC_SubAcct_ID()          != creditLine.getC_SubAcct_ID())
			|| (debitLine.get_ValueAsInt("C_Tax_ID") != creditLine.get_ValueAsInt("C_Tax_ID"))
			|| (debitLine.getC_UOM_ID()              != creditLine.getC_UOM_ID())
			|| (debitLine.getM_Product_ID()          != creditLine.getM_Product_ID())
			|| (debitLine.getUser1_ID()              != creditLine.getUser1_ID())
			|| (debitLine.getUser2_ID()              != creditLine.getUser2_ID())
		   )
			throw new AdempiereException(Msg.getMsg(journal.getCtx(), "BXS_DATEV_JournalDimensionsMustBeEqual"));

	}
	
} // EventHandler
