package sstation

class AcUser implements Serializable {

	private static final long serialVersionUID = 1

	transient springSecurityService

	String username
	String password
	boolean enabled = true
	boolean accountExpired
	boolean accountLocked
	boolean passwordExpired

	AcUser(String username, String password) {
		this()
		this.username = username
		this.password = password
	}

	@Override
	int hashCode() {
		username?.hashCode() ?: 0
	}

	@Override
	boolean equals(other) {
		is(other) || (other instanceof AcUser && other.username == username)
	}

	@Override
	String toString() {
		username
	}

	Set<AcRole> getAuthorities() {
		AcUserAcRole.findAllByAcUser(this)*.acRole
	}

	def beforeInsert() {
		encodePassword()
	}

	def beforeUpdate() {
		if (isDirty('password')) {
			encodePassword()
		}
	}

	protected void encodePassword() {
		// Never silently store a plaintext password. If springSecurityService isn't
		// wired (e.g. too early in boot) the bcrypt check on login would later fail
		// against the plaintext value — fail loudly instead (TC-008).
		if (springSecurityService == null) {
			throw new IllegalStateException("springSecurityService not wired — cannot save AcUser")
		}
		password = springSecurityService.encodePassword(password)
	}

	static transients = ['springSecurityService']

	static constraints = {
		username blank: false, unique: true
		password blank: false
	}

	static mapping = {
		password column: '`password`'
	}
}
