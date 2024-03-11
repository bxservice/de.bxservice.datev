package de.bxservice.datev.process;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

public class ProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		if (DATEV_SSV_Export.class.getName().equals(className))
			return new DATEV_SSV_Export();

		return null;
	}

}
