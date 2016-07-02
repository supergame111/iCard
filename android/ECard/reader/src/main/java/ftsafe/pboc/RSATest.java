package ftsafe.pboc;

import java.math.*;
import java.util.*;
class RSA {
	public BigInteger n;
	public BigInteger d;	//私钥
	public BigInteger e;	//公钥
	
	public RSA(BigInteger p, BigInteger q, BigInteger e) {
		n = p.multiply(q);
		this.e = e;
		// e * d mod (p-1)*(q-1) = 1;
		d = e.modInverse((p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE)));
		System.out.println("n: " + n);
		System.out.println("d: " + d);
	}
	public BigInteger encoding(BigInteger m) {
		return m.modPow(e, n);
	}
	public BigInteger decoding(BigInteger c) {
		return c.modPow(d, n);
	}
}
public class RSATest {
	public static void main(String[] args) {
//		if (args.length < 3) { return; }
		RSA rsa = new RSA(new BigInteger("3"), new BigInteger("7"), new BigInteger("10001",16));
		Scanner cin = new Scanner(System.in);
		while (cin.hasNext()) {
			BigInteger m = new BigInteger(cin.next());
			BigInteger e = rsa.encoding(m);
			System.out.println("E: " + e);
			System.out.println("D: " + rsa.decoding(e));
		}
	}
}