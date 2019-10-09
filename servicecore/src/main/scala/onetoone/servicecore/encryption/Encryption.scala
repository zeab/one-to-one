package onetoone.servicecore.encryption

//Imports
import onetoone.servicecore.AppConf
//Java
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object Encryption {

  implicit class Encryption(val text: String) {
    def encrypt: String = {
      // Create key and cipher
      val aesKey: SecretKeySpec = new SecretKeySpec(AppConf.encryptionKey.getBytes, "AES")
      val cipher: Cipher = Cipher.getInstance("AES")
      // encrypt the text
      cipher.init(Cipher.ENCRYPT_MODE, aesKey)
      cipher.doFinal(text.getBytes).mkString(",")
    }
    def decrypt: String = {
      // Create key and cipher
      val aesKey: SecretKeySpec = new SecretKeySpec(AppConf.encryptionKey.getBytes, "AES")
      val cipher: Cipher = Cipher.getInstance("AES")
      // decrypt the text
      cipher.init(Cipher.DECRYPT_MODE, aesKey)
      new String(cipher.doFinal(text.split(",").map(_.toByte)))
    }
  }

}
