
#include "stdio.h"
#include "ModuleMgr.h"
#include "Module.h"

int main(int argc, char* argv[])
{
	ModuleMgr* mgr = ModuleMgr::GetInst();
	int mcnt = mgr->GetModulesCnt();
	printf("modules count = %d\n", mcnt);
	mgr->PrintAllModules();

	Module* m = mgr->FindModule("Module2");
	if(m){
		m->Read();
	}
	return 0;
}
