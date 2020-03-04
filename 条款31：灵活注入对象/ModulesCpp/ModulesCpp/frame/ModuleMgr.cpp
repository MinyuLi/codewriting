
#include "ModuleMgr.h"
#include "Module.h"
#include "assert_exit.h"

ModuleMgr::ModuleMgr()
{
}

ModuleMgr::~ModuleMgr()
{
}

ModuleMgr* ModuleMgr::GetInst()
{
	static ModuleMgr mgr;
	return &mgr;
}


int ModuleMgr::GetModulesCnt()
{
	return m_MM.size();
}

int ModuleMgr::RegisterModule(Module* m)
{
	ModuleMap::iterator mmi;
	string name = m->GetName();
	mmi = m_MM.find(name);
	if( mmi != m_MM.end() ){
		printf("module %s already register.", name.c_str());
		assert_exit(0); //代码看护,不允许有两个相同的modules
		return -1;
	}
	m_MM.insert(ModuleMapPair(name, m));
	return 0;
}

Module* ModuleMgr::FindModule(const char* name)
{
	ModuleMap::iterator mmi;
	mmi = m_MM.find(name);
	if( mmi != m_MM.end() ){
		return mmi->second;
	}
	return NULL;
}

void ModuleMgr::PrintAllModules()
{
	ModuleMap::iterator mmi;
	for(mmi = m_MM.begin(); mmi != m_MM.end(); mmi++){
		printf("module name: %s\n", mmi->first.c_str());
	}
}