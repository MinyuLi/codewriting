#include "Module.h"
#include "ModuleMgr.h"

Module::Module(const char* name)
{
	m_Name = name;
	ModuleMgr* mgr = ModuleMgr::GetInst();
	if(mgr){
		mgr->RegisterModule(this);
	}
}

Module::~Module(void)
{
}

string Module::GetName()
{
	return m_Name;
}
