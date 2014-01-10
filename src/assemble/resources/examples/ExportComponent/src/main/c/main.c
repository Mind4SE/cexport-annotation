#include "clt_export.h"
#include "srv_export.h"

int main(void)
{
	return srv_getNumber();
}

uint8_t clt_getNumber(void)
{
	return 18;
}
