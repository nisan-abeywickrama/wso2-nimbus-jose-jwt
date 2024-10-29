/*
 * nimbus-jose-jwt
 *
 * Copyright 2012-2016, Connect2id Ltd and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.nimbusds.jose.crypto;


import java.text.ParseException;

import junit.framework.TestCase;
import org.junit.Assert;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.ByteUtils;


/**
 * @author Vladimir Dzhuvinov
 * @version 2021-01-28
 */
public class ECDSATranscodingTest extends TestCase {
	
	// https://bitbucket.org/connect2id/nimbus-jose-jwt/issues/399/
	public void testRejectIllegalSignatureSizesBeforeTranscodeToDER_oneByteOff()
		throws JOSEException, ParseException {
		
		for (JWSAlgorithm alg: JWSAlgorithm.Family.EC) {
			
			ECKey ecJWK = new ECKeyGenerator(Curve.forJWSAlgorithm(alg).iterator().next())
				.algorithm(alg)
				.generate();
			
			JWSObject jwsObject = new JWSObject(new JWSHeader(alg), new Payload("Elliptic cure"));
			
			jwsObject.sign(new ECDSASigner(ecJWK));
			
			String string = jwsObject.serialize();
			
			JWSObject parsedJWSObject = JWSObject.parse(string);
			
			// Append extra byte to signature portion
			// (don't simply append char to base64url - not
			// guaranteed to modify the base64url encoded bytes!)
			String modifiedString =
				parsedJWSObject.getParsedParts()[0].toString() + // header
				"." +
				parsedJWSObject.getParsedParts()[1].toString() + // payload
				"." +
				Base64URL.encode(ByteUtils.concat(parsedJWSObject.getParsedParts()[2].decode(), new byte[]{(byte)'X'})) // append extra char
				;
			
			JWSObject modifiedJWSObject = JWSObject.parse(modifiedString);
			
			assertFalse("Signature rejected", modifiedJWSObject.verify(new ECDSAVerifier(ecJWK.toECPublicKey())));
		}
	}
	
	
	// https://bitbucket.org/connect2id/nimbus-jose-jwt/issues/399/
	public void testTranscodingFunWithBase64URL()
		throws JOSEException, ParseException {
		
		// ES256
		ECKey ec256JWK = new ECKeyGenerator(Curve.P_256)
			.generate();
		
		JWSObject es256 = new JWSObject(new JWSHeader(JWSAlgorithm.ES256), new Payload("Elliptic cure"));
		
		es256.sign(new ECDSASigner(ec256JWK));
		
		String s = es256.serialize();
		
		// Append extra char to final signature portion
		JWSObject es256mod = JWSObject.parse(s + "X");
		
		assertFalse("Signature rejected", es256mod.verify(new ECDSAVerifier(ec256JWK.toECPublicKey())));
		
		// ES384
		ECKey ec384JWK = new ECKeyGenerator(Curve.P_384)
			.generate();
		
		JWSObject es384 = new JWSObject(new JWSHeader(JWSAlgorithm.ES384), new Payload("Elliptic cure"));
		
		es384.sign(new ECDSASigner(ec384JWK));
		
		s = es384.serialize();
		
		// Append extra char to final signature portion
		JWSObject es384mod = JWSObject.parse(s + "X");
		
		// Horror, what's going on?!?
		assertTrue("Signature accepted", es384mod.verify(new ECDSAVerifier(ec384JWK.toECPublicKey())));
		
		// Appending an extra char at the longer BASE64URL text for ES384
		// (and ES512) doesn't actually change the underlying signature bytes :)
		Assert.assertArrayEquals(es384.getSignature().decode(), es384mod.getSignature().decode());
	}
}
