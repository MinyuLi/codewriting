package org.test;

import org.lmy.module.IF.ModuleMgr;
import org.lmy.module.IF.IModule;

public class ModulesJavaTest {
	public static void ReadData(String moduleName){
		ModuleMgr mgr = ModuleMgr.GetInst();
		IModule m = mgr.FindModule(moduleName);
		if(m == null){
			System.out.println("Find " + moduleName + " fail.");
			return;
		}
		m.Read();
	}
	public static void main(String[] args)throws Exception
	{
		ReadData("Module_2");
	}
}