#include <stdio.h>

int doo(int a, int b, int c, float d, float e, int f, int g, int h, int i){
	if(d < e){
		return (a*b*c + f*g*h*i);
	}else if(d > e){
		return (a*b*c + f*g*h*i + 1);
	}
	return 0;
}


int main(){
	int r0,r1,r2,rr0,rr1,rr2,rr3;
	float f0,f1;

	r0 = 2;
	r1 = 3;
	r2 = 4;
	rr0 = 20;
	rr1 = 30;
	rr2 = 6;
	rr3 = 7;
	f0 = 1.1;
	f1 = 78.2098;

	int j1,j2,j3,j4;
	j1 = doo(r0,r1,r2,f0,f1,rr0,rr1,rr2,rr3);
	j2 = doo(r0,r1,r2,f0,f1,rr0,rr1,rr2,rr3);
	j3 = doo(r0,r1,r2,1.0,1.0,rr0,rr1,rr2,rr3);
	j4 = doo(r0+r1-r2, r0*r1*r2, r1,  f0+f1+f0*f1, f1+f0*f1+f0+100.0, rr0, rr1, rr2*rr3/rr2/rr0+(rr0*(rr1+rr2)), doo(rr0,rr0,rr0,f0,f1,rr0,rr0,rr0,rr0));


	printf("%d, %d, %d, %d\n", j1,j2,j3,j4);
	return 0;
}

