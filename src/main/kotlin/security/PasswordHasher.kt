package security

import de.mkammerer.argon2.Argon2Factory

class PasswordHasher {

    private val argon2 = Argon2Factory.create()

    fun hash(password: String): String {
        require(password.length >= 8) {
            "Password must contain at least 8 characters"
        }

        return argon2.hash(
            3,
            64 * 1024,
            1,
            password.toCharArray()
        )
    }

    fun verify(hash: String, password: String): Boolean {
        return argon2.verify(hash, password.toCharArray())
    }
}