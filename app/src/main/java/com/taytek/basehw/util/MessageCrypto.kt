package com.taytek.basehw.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Uçtan uca şifreleme için AES-256-GCM tabanlı mesaj şifreleme/çözme utility.
 * 
 * Anahtar üretimi: İki UID'nin birleşik SHA-256 hash'i kullanılır.
 * Bu sayede her iki taraf da aynı anahtarı bağımsız olarak üretebilir.
 * 
 * Format: [IV (12 byte)] + [Şifreli metin] -> Base64 olarak saklanır
 */
object MessageCrypto {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // bit
    private const val IV_LENGTH = 12 // byte (GCM için önerilen)

    /**
     * İki kullanıcı UID'sinden ortak şifreleme anahtarı üretir.
     * Deterministik: aynı UID çifti her zaman aynı anahtarı üretir.
     */
    fun generateConversationKey(uidA: String, uidB: String): ByteArray {
        // Tutarlı sıralama: alfabetik küçük önce
        val first = if (uidA <= uidB) uidA else uidB
        val second = if (uidA <= uidB) uidB else uidA
        
        val combined = "$first:$second"
        val sha256 = MessageDigest.getInstance("SHA-256")
        return sha256.digest(combined.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Mesajı şifreler. Sonuç Base64 string olarak döner.
     * Format: Base64(IV + ciphertext)
     */
    fun encrypt(plaintext: String, key: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        
        // IV + ciphertext birleştir
        val combined = iv + ciphertext
        return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
    }

    /**
     * Şifreli mesajı çözer.
     * Giriş: Base64(IV + ciphertext)
     * Çıkış: plaintext string
     */
    fun decrypt(encryptedBase64: String, key: ByteArray): String {
        val combined = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
        
        if (combined.size < IV_LENGTH + 1) {
            throw IllegalArgumentException("Şifreli mesaj çok kısa")
        }
        
        // IV'yi ayır
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
        
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, StandardCharsets.UTF_8)
    }

    /**
     * Bir string'in şifreli olup olmadığını kontrol eder.
     * Şifreli mesajlar Base64 formatında ve belirli bir minimum uzunluktadır.
     */
    fun isEncrypted(text: String): Boolean {
        // Base64 kontrolü ve minimum uzunluk (IV + minimum ciphertext)
        if (text.length < 30) return false
        return try {
            val decoded = android.util.Base64.decode(text, android.util.Base64.DEFAULT)
            decoded.size >= IV_LENGTH + 1
        } catch (e: Exception) {
            false
        }
    }
}