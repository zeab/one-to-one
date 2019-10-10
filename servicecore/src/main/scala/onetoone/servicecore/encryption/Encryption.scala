package onetoone.servicecore.encryption

//Imports
import onetoone.servicecore.AppConf
//Java
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object Encryption {

  implicit class Encryption(val text: String) {
    def encrypt: String = {
      // Create key and cipher
      val aesKey: SecretKeySpec = new SecretKeySpec(AppConf.encryptionKey.getBytes, "AES")
      val cipher: Cipher = Cipher.getInstance("AES")
      // encrypt the text
      cipher.init(Cipher.ENCRYPT_MODE, aesKey)
      Base64.getEncoder.encodeToString( cipher.doFinal(text.getBytes))
    }
    def decrypt: String = {
      val decodedText: Array[Byte] = new String(Base64.getDecoder.decode(text)).split(",").map(_.toByte)
      // Create key and cipher
      val aesKey: SecretKeySpec = new SecretKeySpec(AppConf.encryptionKey.getBytes, "AES")
      val cipher: Cipher = Cipher.getInstance("AES")
      // decrypt the text
      cipher.init(Cipher.DECRYPT_MODE, aesKey)
      new String(cipher.doFinal(decodedText))
    }
  }

}
