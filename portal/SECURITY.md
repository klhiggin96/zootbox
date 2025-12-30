# Security Documentation

## Security Model

The ZootBox Inventory Portal is designed for deployment in a **secure, isolated network environment** with the following security characteristics:

### Network Security

- **Deployment**: Portal runs on a dedicated operator PC within a Tailscale VPN mesh network
- **Access Control**: Network-level security via Tailscale authentication and access controls
- **No Public Internet**: Portal is NOT accessible from the public internet
- **Trusted Network**: All clients connecting to the portal are trusted machines within the VPN

### Authentication & Authorization

**Current Implementation:**
- ✅ **No authentication required** - Access control handled at network level by Tailscale VPN
- ✅ **Single operator model** - Portal designed for single-user operation on trusted PC
- ✅ **Network isolation** - VPN provides cryptographic security and access control

**Design Decision:**
Given the deployment environment (single operator PC, VPN-only access, trusted network), implementing application-level authentication would provide minimal security benefit while adding operational complexity. The network-level security provided by Tailscale VPN is sufficient for this use case.

### XSS (Cross-Site Scripting) Protection

**Implemented Protections:**
- ✅ **HTML Escaping**: All user-provided data is escaped using `escapeHtml()` utility before rendering
- ✅ **Safe DOM Manipulation**: Template literals with escaped variables prevent script injection
- ✅ **Input Validation**: All form inputs validated before processing

**Protected Areas:**
- Coil IDs (A1-J1)
- Product names and icons
- Machine names and endpoint URLs
- Status messages
- All user-editable fields

**Implementation:**
```javascript
import { escapeHtml } from './utils/validation.js';

// Safe rendering
element.innerHTML = `<span>${escapeHtml(userInput)}</span>`;
```

### CORS (Cross-Origin Resource Sharing)

**Current Configuration:**
- Backend accepts requests from `localhost` origins only
- Production deployment uses same-origin policy (portal and backend on same machine)
- Remote access via Tailscale VPN maintains same-origin relationship

### Data Storage Security

**LocalStorage:**
- ✅ **Plaintext storage** - Machine configurations stored in browser localStorage
- ✅ **Non-sensitive data** - Only stores machine names, endpoint URLs, UI preferences
- ✅ **No credentials** - No passwords or API keys stored
- ✅ **VPN-secured** - Physical access to operator PC required

**Design Decision:**
Encrypting localStorage data provides minimal security benefit in this deployment model. If an attacker has physical or remote access to the operator PC, they already have access to the backend and inventory data. The complexity of key management outweighs the security benefit.

**What's NOT Stored:**
- ❌ Passwords or credentials
- ❌ API keys or tokens
- ❌ Sensitive business data
- ❌ Personal information

### Content Security Policy (CSP)

**Current Implementation:**
- Uses Tailwind CSS CDN (intentional design decision for simplicity)
- Material Symbols icons from Google Fonts CDN
- All application code served from same origin

**Production Recommendations:**
- Consider self-hosting Tailwind CSS and icon fonts for full CSP compliance
- Implement `Content-Security-Policy` header if additional defense-in-depth desired

### Logging & Debug Information

**Development Mode:**
- Console logging enabled for debugging
- Verbose error messages
- Environment detection via hostname (`localhost` = development)

**Production Mode:**
- ✅ All `console.log/warn/info/debug` statements stripped during build
- ✅ `console.error` preserved for critical issues
- ✅ Error messages sanitized to avoid information leakage
- ✅ Build process removes comments and debug code

**Enable Debug Logging in Production:**
```javascript
// In browser console:
localStorage.setItem('DEBUG', 'true');
location.reload();
```

### Known Limitations & Risks

#### 1. No Application-Level Authentication
**Risk**: Anyone with VPN access can use the portal
**Mitigation**: Tailscale VPN provides strong authentication and access control
**Acceptance**: Acceptable for single-operator, trusted-network deployment

#### 2. Plaintext LocalStorage
**Risk**: Machine configurations visible in browser storage
**Mitigation**: No sensitive data stored; VPN-secured network
**Acceptance**: Acceptable given deployment model and data sensitivity

#### 3. CDN Dependencies
**Risk**: Tailwind CSS and Material Symbols loaded from external CDNs
**Mitigation**: Subresource Integrity (SRI) not currently implemented
**Recommendation**: Self-host in production for full supply chain security

#### 4. No Rate Limiting
**Risk**: Backend API has no rate limiting
**Mitigation**: Trusted network; single operator
**Acceptance**: Acceptable for internal tool; can implement if needed

### Security Checklist for Deployment

- [ ] Verify portal is only accessible via Tailscale VPN
- [ ] Confirm backend is not exposed to public internet
- [ ] Test that only authorized Tailscale users can access
- [ ] Review Tailscale ACLs to restrict access as needed
- [ ] Verify build process strips console.log statements
- [ ] Confirm XSS protections are active (check escapeHtml usage)
- [ ] Backup machine configurations from localStorage
- [ ] Document emergency access procedures

### Incident Response

**If Unauthorized Access Suspected:**
1. Review Tailscale access logs
2. Check backend audit logs (if implemented)
3. Verify operator PC has not been compromised
4. Rotate Tailscale auth keys if necessary
5. Review machine configurations for tampering

**If XSS Vulnerability Found:**
1. Identify affected component
2. Verify `escapeHtml()` is used for all user inputs
3. Test with malicious payloads: `<script>alert('xss')</script>`
4. Rebuild and redeploy if fixes required

### Security Contact

For security concerns or vulnerability reports:
- **Internal**: Contact system administrator
- **External**: Not applicable (internal tool, not publicly accessible)

### Security Audit History

| Date | Auditor | Scope | Findings |
|------|---------|-------|----------|
| 2025-12-29 | Claude Code | Production Readiness | XSS vulnerabilities fixed, logging cleaned up |

### Future Security Enhancements

**Optional Improvements:**
- [ ] Implement Content Security Policy (CSP) headers
- [ ] Self-host Tailwind CSS and icon fonts
- [ ] Add Subresource Integrity (SRI) for CDN resources
- [ ] Implement backend API rate limiting
- [ ] Add audit logging for inventory changes
- [ ] Consider application-level authentication if multi-user deployment needed

### References

- [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [Tailscale Security Model](https://tailscale.com/security/)
- [localStorage Security Best Practices](https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API/Using_the_Web_Storage_API#security)
