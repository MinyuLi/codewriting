package org.lmy.module.IF;

public class ModuleMgr {
	private ModuleMgr(){}
	private static ModuleMgr mMgr = null;
	public static ModuleMgr GetInst(){
		if(mMgr == null){
			mMgr = new ModuleMgr();
		}
		return mMgr;
	}
	
	public IModule FindModule(String moduleName){
		String moduleClassName = "org.lmy.module.modules." + moduleName;
		try {
			Class<?> clazz = Class.forName(moduleClassName);
			IModule m = (IModule)clazz.newInstance();
			return m;
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		 catch (InstantiationException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
