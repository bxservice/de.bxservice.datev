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
package de.bxservice.datev.process;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.compiere.model.MProcessPara;
import org.compiere.model.MQuery;
import org.compiere.model.MTable;
import org.compiere.model.PrintInfo;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

/**
 * SSV Exporter for DATEV
 */
public class DATEV_SSV_Export extends SvrProcess{

	/* Datum */
	private Timestamp p_BX_DATEV_Datum = null;
    private Timestamp p_BX_DATEV_Datum_To = null;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			switch (name) {
			case "BX_DATEV_Datum":
				p_BX_DATEV_Datum = para.getParameterAsTimestamp();
				p_BX_DATEV_Datum_To = para.getParameter_ToAsTimestamp();
				break;
			default:
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
			}
		}
	}

	@Override
	protected String doIt() throws Exception {
		MPrintFormat pf = MPrintFormat.get (Env.getCtx(), 1000034, true); // DATEV Export (Template) / 43841009-8a28-4e08-8052-72e9a6899398
		MQuery query = new MQuery(MTable.getTableName(getCtx(), pf.get_Table_ID()));
		query.addRangeRestriction("BX_DATEV_Datum", p_BX_DATEV_Datum, p_BX_DATEV_Datum_To);
		PrintInfo info = new PrintInfo(pf.getName(), pf.getAD_Table_ID(), 0);
		info.setDescription(query.getInfo());
		ReportEngine re = new ReportEngine (Env.getCtx(), pf, query, info, get_TrxName());

		String datumTo = new SimpleDateFormat("yyyy-MM-dd").format(p_BX_DATEV_Datum_To);
		Path tmpFolder = Files.createTempDirectory("DATEV");
		StringBuilder tmpFile = new StringBuilder()
			.append(tmpFolder.toAbsolutePath().toString()).append(File.separator)
			.append("RV_BX_DATEV_").append(datumTo).append(".csv");
		File file = new File(tmpFile.toString());
		re.createCSV(file, ';', re.getPrintFormat().getLanguage());
		if (processUI != null) {
			processUI.download(file);
		} else if( getProcessInfo() != null ) {
			ProcessInfo m_pi = getProcessInfo();
			m_pi.setExport(true);
			m_pi.setExportFile(file);
			m_pi.setExportFileExtension("csv");
		}

		return "OK";
	}

}
