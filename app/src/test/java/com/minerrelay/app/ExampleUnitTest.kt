package com.minerrelay.app

import com.minerrelay.app.mining.AcceptedShareRecord
import com.minerrelay.app.mining.BitcoinCrypto
import com.minerrelay.app.mining.MiningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class ExampleUnitTest {

    @Test
    fun `doubleSha256 matches known Bitcoin test vector`() {
        val digest = BitcoinCrypto.doubleSha256("hello".toByteArray(Charsets.UTF_8))
        assertEquals(
            "9595c9df90075148eb06860365df33584b75bff782a510c6cd4883a419833d50",
            BitcoinCrypto.bytesToHex(digest)
        )
    }

    @Test
    fun `bitcoin genesis block header with nonce 2083236893 produces exact genesis hash`() {
        val header = BitcoinCrypto.buildBlockHeaderFromDisplayHex(
            version = 1L,
            prevHashDisplayHex = "0000000000000000000000000000000000000000000000000000000000000000",
            merkleRootDisplayHex = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
            ntime = 1231006505L,
            nbits = 0x1d00ffffL,
            nonce = 2083236893L
        )
        assertEquals(80, header.size)

        val hashDisplay = BitcoinCrypto.headerHashDisplayHex(header)
        assertEquals(
            "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f",
            hashDisplay
        )

        val genesisTarget = BitcoinCrypto.nbitsToTarget(0x1d00ffffL)
        assertEquals(BitcoinCrypto.DIFFICULTY_1_TARGET, genesisTarget)
        assertTrue(BitcoinCrypto.verifyHeaderAgainstTarget(header, genesisTarget))

        val invalidNonceHeader = BitcoinCrypto.buildBlockHeaderFromDisplayHex(
            version = 1L,
            prevHashDisplayHex = "0000000000000000000000000000000000000000000000000000000000000000",
            merkleRootDisplayHex = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b",
            ntime = 1231006505L,
            nbits = 0x1d00ffffL,
            nonce = 2083236894L
        )
        assertFalse(BitcoinCrypto.verifyHeaderAgainstTarget(invalidNonceHeader, genesisTarget))
    }

    @Test
    fun `merkle root and stratum 80-byte header assembly is deterministic and exact`() {
        val coinb1 = "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0403000000"
        val en1 = "00000001"
        val en2 = "00000002"
        val coinb2 = "ffffffff0100f2052a010000001976a914000000000000000000000000000000000000000088ac00000000"
        val branch = listOf("1111111111111111111111111111111111111111111111111111111111111111")

        val expectedCoinbaseHash = BitcoinCrypto.doubleSha256(BitcoinCrypto.hexToBytes(coinb1 + en1 + en2 + coinb2))
        val expectedMerkle = BitcoinCrypto.doubleSha256(expectedCoinbaseHash + BitcoinCrypto.hexToBytes(branch[0]))

        val merkle = BitcoinCrypto.computeMerkleRootInternal(coinb1, en1, en2, coinb2, branch)
        assertEquals(BitcoinCrypto.bytesToHex(expectedMerkle), BitcoinCrypto.bytesToHex(merkle))

        val prevHashStratum = "0000000011111111222222223333333344444444555555556666666677777777"
        val header = BitcoinCrypto.buildBlockHeaderFromStratum(
            versionHex = "20000000",
            stratumPrevHashHex = prevHashStratum,
            coinb1Hex = coinb1,
            extranonce1Hex = en1,
            extranonce2Hex = en2,
            coinb2Hex = coinb2,
            merkleBranchHexList = branch,
            ntimeHex = "65000001",
            nbitsHex = "1d00ffff",
            nonce = 42L
        )
        assertEquals(80, header.size)
        // version little-endian of 0x20000000 -> 00 00 00 20
        assertEquals("00000020", BitcoinCrypto.bytesToHex(header.copyOfRange(0, 4)))
        // nonce little-endian of 42 (0x2a) -> 2a 00 00 00
        assertEquals("2a000000", BitcoinCrypto.bytesToHex(header.copyOfRange(76, 80)))
    }

    @Test
    fun `difficultyToTarget scales inversely with difficulty`() {
        val diff1 = BitcoinCrypto.difficultyToTarget(1.0)
        assertEquals(BitcoinCrypto.DIFFICULTY_1_TARGET, diff1)

        val diff2 = BitcoinCrypto.difficultyToTarget(2.0)
        assertEquals(diff1.divide(BigInteger.valueOf(2)), diff2)
    }

    @Test
    fun `measured hashrate is strictly zero with no accepted shares and computes accurately otherwise`() {
        val now = 1_000_000L
        assertEquals(
            0.0,
            MiningEngine.calculateHashrateHps(emptyList(), MiningEngine.WINDOW_15M_MS, now, now - 60_000L),
            0.0
        )

        val oneShare = listOf(AcceptedShareRecord(timestampMs = now - 10_000L, difficulty = 1.0))
        val hps = MiningEngine.calculateHashrateHps(oneShare, MiningEngine.WINDOW_15M_MS, now, now - 100_000L)
        // diff=1 (2^32 hashes) over 100 seconds = 42,949,672.96 H/s
        assertEquals(42_949_672.96, hps, 1.0)
    }
}
