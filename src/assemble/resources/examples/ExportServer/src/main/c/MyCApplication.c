#include "myItf_export.h"

int main(void)
{
	if (myItf_myBoolMethod())
		return myItf_myUInt8_tMethod();
	else
		return -myItf_myUInt8_tMethod();
}
