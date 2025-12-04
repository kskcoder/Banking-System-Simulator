package com.tejas.authservice;

import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;

public class OtpUtils {
	public static String otpGenerator() {
	    Random random = new Random();
	    return String.format("%06d", random.nextInt(1000000));
	}
	
	public static String hash(String data) {
	    return DigestUtils.sha256Hex(data);
	}
}
