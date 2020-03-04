#pragma once
#include <atlsync.h>


class CCriticalSectionLock
{
	// Constructors
public:
	explicit CCriticalSectionLock(ATL::CCriticalSection &cs)
	{
		m_pCS=&cs;
		m_pCS->Enter();
	}
	~CCriticalSectionLock()
	{
		m_pCS->Leave();
	}

protected:
	ATL::CCriticalSection* m_pCS;
};

#define DoLock(x)  CCriticalSectionLock x##_lock(x)