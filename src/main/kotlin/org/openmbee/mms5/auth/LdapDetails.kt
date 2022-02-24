package org.openmbee.mms5.auth

import io.ktor.auth.*
import java.util.*
import javax.naming.Context
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult

data class UserDetailsPrincipal(val name: String, val groups: List<String>) : Principal

/**
 * Do LDAP authentication and verify [credential] by [doVerify] function
 */
fun <K : Credential, P : Any> ldapAuthenticate(
    credential: K,
    ldapServerURL: String,
    ldapEnvironmentBuilder: (MutableMap<String, Any?>) -> Unit = {},
    doVerify: InitialDirContext.(K) -> P?
): P? {
    return try {
        val root = ldapLogin(ldapServerURL, ldapEnvironmentBuilder)
        try {
            root.doVerify(credential)
        } finally {
            root.close()
        }
    } catch (ne: NamingException) {
        null
    }
}

/**
 * Do LDAP authentication and verify [UserPasswordCredential] by [validate] function and construct [UserDetailsPrincipal]
 */
fun ldapAuthenticate(
    credential: UserPasswordCredential,
    ldapServerURL: String,
    userDNFormat: String,
    validate: InitialDirContext.(UserPasswordCredential) -> UserDetailsPrincipal?
): UserDetailsPrincipal? {
    val configurator: (MutableMap<String, Any?>) -> Unit = { env ->
        env[Context.SECURITY_AUTHENTICATION] = "simple"
        env[Context.SECURITY_PRINCIPAL] = userDNFormat.format(ldapEscape(credential.name))
        env[Context.SECURITY_CREDENTIALS] = credential.password
    }

    return ldapAuthenticate(credential, ldapServerURL, configurator, validate)
}

/**
 * Do LDAP authentication and verify [UserPasswordCredential] by [userDNFormat] and construct [UserDetailsPrincipal]
 */
fun ldapAuthenticate(
    credential: UserPasswordCredential,
    ldapServerURL: String,
    userDNFormat: String,
    ldapBase: String,
    groupAttribute: String,
    groupFilter: String,
): UserDetailsPrincipal? {
    return ldapAuthenticate(credential, ldapServerURL, userDNFormat) {
        val sc = SearchControls()
        sc.returningAttributes = arrayOf(groupAttribute)
        sc.searchScope = SearchControls.SUBTREE_SCOPE

        val resultList: List<String> = this.search(ldapBase, groupFilter, sc).mapAttrToString(groupAttribute) ?: emptyList()
        UserDetailsPrincipal(it.name, resultList)
    }
}

private fun <T> NamingEnumeration<T>.mapAttrToString(attrString: String): List<String> {
    val newList = mutableListOf<String>()
    while (this.hasMore()) {
        val sr = this.next() as SearchResult
        newList.add(sr.nameInNamespace)
    }
    return newList
}

private fun ldapLogin(ldapURL: String, ldapEnvironmentBuilder: (MutableMap<String, Any?>) -> Unit): InitialDirContext {
    val env = Hashtable<String, Any?>()
    env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
    env[Context.PROVIDER_URL] = ldapURL

    ldapEnvironmentBuilder(env)

    return InitialDirContext(env)
}

internal fun ldapEscape(string: String): String {
    for (index in 0..string.lastIndex) {
        val character = string[index]
        if (character.shouldEscape()) {
            return ldapEscapeImpl(string, index)
        }
    }

    return string
}

private fun ldapEscapeImpl(string: String, firstIndex: Int): String = buildString {
    var lastIndex = 0
    for (index in firstIndex..string.lastIndex) {
        val character = string[index]
        if (character.shouldEscape()) {
            append(string, lastIndex, index)
            if (character in ESCAPE_CHARACTERS) {
                append('\\')
                append(character)
            } else {
                character.toString().toByteArray().let { encoded ->
                    for (element in encoded) {
                        val unsignedValue = element.toInt() and 0xff
                        append('\\')
                        append(unsignedValue.toString(16).padStart(2, '0'))
                    }
                }
            }

            lastIndex = index + 1
        }
    }

    append(string, lastIndex, string.length)
}

private val ESCAPE_CHARACTERS = charArrayOf(' ', '"', '#', '+', ',', ';', '<', '=', '>', '\\')

private fun Char.shouldEscape(): Boolean = this.code.let { codepoint ->
    when (codepoint) {
        in 0x3f..0x7e -> codepoint == 0x5c // the only forbidden character is backslash
        in 0x2d..0x3a -> false // minus, point, slash (allowed), digits + colon :
        in 0x24..0x2a -> false // $%&'()*
        0x21 -> false // exclamation
        else -> true
    }
}
