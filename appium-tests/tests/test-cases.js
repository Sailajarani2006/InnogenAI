// ----------------------------------------------------
// GENERATE THE 305 TEST CASES PARAMETRICALLY FOR MOBILE APPIUM
// ----------------------------------------------------
const testCases = [];

// Category 1: Valid Logins (35 cases)
const validEmails = [
    'admin@innogen.com', 'user@innogen.com', 'manager@innogen.com', 'developer@innogen.com',
    'test.user@innogen.com', 'support@innogen.com', 'billing@innogen.com', 'demo@innogen.com',
    'api@innogen.com', 'guest@innogen.com'
];
const validDomains = ['gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com', 'co.uk', 'org', 'net'];
for (let i = 1; i <= 35; i++) {
    const email = i <= validEmails.length 
        ? validEmails[i - 1] 
        : `user.${i}@${validDomains[i % validDomains.length]}`;
    testCases.push({
        id: `APP-VAL-${String(i).padStart(3, '0')}`,
        category: 'Valid Login',
        name: `Valid Mobile Login Case ${i} - ${email.split('@')[0]}`,
        email: email,
        password: `ValidPassword${i}!`,
        expectedStatus: 'success',
        description: `Verify that a user with valid email '${email}' and a standard compliant password is authenticated successfully.`
    });
}

// Category 2: Field Validation (100 cases)
const invalidEmails = [
    '', // empty
    'plainaddress', // no @
    '#@%^%#$@#$@#.com', // junk
    '@domain.com', // no username
    'Joe Smith <email@domain.com>', // name in angle brackets
    'email.domain.com', // no @
    'email@domain@domain.com', // double @
    '.email@domain.com', // leading dot
    'email.@domain.com', // trailing dot in username
    'email..other@domain.com', // double dot
    'email@domain.com (Joe Smith)', // extra text
    'email@domain', // missing tld
    'email@111.222.333.44444', // invalid IP
    'email@domain.com.', // trailing dot
    'email@domain.com..', // double trailing dot
];
const shortPasswords = ['1', '12', '123', '1234', '12345', '12346', '1234567'];
const weakPasswords = [
    'nouppercase123!',
    'NOLOWERCASE123!',
    'NoSpecialChar123',
    'NoNumbersHere!',
    '      ', // spaces only
];

let valCount = 1;
testCases.push({
    id: `APP-VAL-ERR-${String(valCount++).padStart(3, '0')}`,
    category: 'Field Validation',
    name: 'Empty Email Address',
    email: '',
    password: 'Password123!',
    expectedStatus: 'error',
    description: 'Verify login fails when the email field is empty on mobile.'
});
testCases.push({
    id: `APP-VAL-ERR-${String(valCount++).padStart(3, '0')}`,
    category: 'Field Validation',
    name: 'Empty Password Field',
    email: 'user@innogen.com',
    password: '',
    expectedStatus: 'error',
    description: 'Verify login fails when the password field is empty on mobile.'
});

// Invalid emails (50 cases)
for (let i = 0; i < 50; i++) {
    const badEmail = invalidEmails[i % invalidEmails.length] + (i >= invalidEmails.length ? `.${i}` : '');
    testCases.push({
        id: `APP-VAL-ERR-${String(valCount++).padStart(3, '0')}`,
        category: 'Field Validation',
        name: `Invalid Email Structure Case ${i + 1}`,
        email: badEmail,
        password: 'Password123!',
        expectedStatus: 'error',
        description: `Verify that an improperly formatted email address '${badEmail}' is rejected by mobile validation.`
    });
}

// Weak/short passwords (48 cases)
for (let i = 0; i < 48; i++) {
    const badPass = i % 2 === 0 
        ? shortPasswords[Math.floor(i / 2) % shortPasswords.length] 
        : weakPasswords[Math.floor(i / 2) % weakPasswords.length] + i;
    testCases.push({
        id: `APP-VAL-ERR-${String(valCount++).padStart(3, '0')}`,
        category: 'Field Validation',
        name: `Weak or Short Password Case ${i + 1}`,
        email: 'user@innogen.com',
        password: badPass,
        expectedStatus: 'error',
        description: `Verify that security validation rejects weak or short password input on mobile.`
    });
}

// Category 3: Security & Injection (100 cases)
const sqlPayloads = [
    "' OR '1'='1",
    "admin' --",
    "admin' #",
    "admin'/*",
    "' or 1=1 --",
    "' or 1=1 or ''='",
    "admin' or '1'='1'--",
    "admin' or '1'='1'#",
    "admin' or '1'='1'/*",
    "'; SAFE_COMMAND --",
    "UNION SELECT NULL, NULL --",
];
const xssPayloads = [
    "<script>alert(1)</script>",
    "\"><script>alert('XSS')</script>",
    "<img src=x onerror=alert(1)>",
    "javascript:alert(1)",
    "<svg/onload=alert(1)>",
    "<iframe src=javascript:alert(1)>",
    "<body onload=alert(1)>",
    "<link rel=stylesheet href=javascript:alert(1)>",
    "<meta http-equiv=\"refresh\" content=\"0;url=javascript:alert(1)\">",
];
const otherPayloads = [
    "../../etc/passwd",
    "C:\\Windows\\system.ini",
    "| cat /etc/passwd",
    "; rm -rf /",
    "&& dir C:\\",
    "{\"username\": {\"$ne\": null}}",
];

let secCount = 1;
for (let i = 0; i < 100; i++) {
    let payload = '';
    let payloadType = '';
    if (i % 3 === 0) {
        payload = sqlPayloads[Math.floor(i / 3) % sqlPayloads.length] + ` /* ${i} */`;
        payloadType = 'SQL Injection';
    } else if (i % 3 === 1) {
        payload = xssPayloads[Math.floor(i / 3) % xssPayloads.length] + ` <!-- ${i} -->`;
        payloadType = 'Cross-Site Scripting (XSS)';
    } else {
        payload = otherPayloads[Math.floor(i / 3) % otherPayloads.length] + ` # ${i}`;
        payloadType = 'Injection/Traversal';
    }
    
    const testEmail = i % 2 === 0 ? payload : 'user@innogen.com';
    const testPass = i % 2 === 0 ? 'Password123!' : payload;
    
    testCases.push({
        id: `APP-SEC-${String(secCount++).padStart(3, '0')}`,
        category: 'Security & Injection',
        name: `${payloadType} Attack Vector Case ${i + 1}`,
        email: testEmail,
        password: testPass,
        expectedStatus: 'error',
        description: `Verify that input validation blocks/sanitizes injection payload: '${payload.substring(0, 20)}...'.`
    });
}

// Category 4: Boundary & Edge Cases (70 cases)
let edgeCount = 1;
// Emojis (15 cases)
for (let i = 1; i <= 15; i++) {
    testCases.push({
        id: `APP-BDY-${String(edgeCount++).padStart(3, '0')}`,
        category: 'Boundary & Edge Cases',
        name: `Unicode/Emoji Input Case ${i}`,
        email: `emoji.${i}@innogen.com`,
        password: `Pass😊🔥Word!${i}`,
        expectedStatus: 'success',
        description: 'Verify mobile login successfully processes Unicode characters and emojis in credentials.'
    });
}

// Extremely long fields (20 cases)
for (let i = 1; i <= 20; i++) {
    const longPass = 'A'.repeat(500 + i * 10) + '1!a';
    testCases.push({
        id: `APP-BDY-${String(edgeCount++).padStart(3, '0')}`,
        category: 'Boundary & Edge Cases',
        name: `Extremely Long Password Case ${i}`,
        email: `user.long.${i}@innogen.com`,
        password: longPass,
        expectedStatus: 'success',
        description: `Verify mobile authentication with a very long password of length ${longPass.length} characters.`
    });
}

// Foreign Languages (15 cases)
const foreignPasswords = [
    'пароль123!', // Russian
    '密码123456!', // Chinese
    'كلمةالمرور123!', // Arabic
    '日本語のパスワード123!', // Japanese
    'passwordहिंदी123!', // Hindi
];
for (let i = 1; i <= 15; i++) {
    const foreignPass = foreignPasswords[i % foreignPasswords.length] + i;
    testCases.push({
        id: `APP-BDY-${String(edgeCount++).padStart(3, '0')}`,
        category: 'Boundary & Edge Cases',
        name: `Internationalized Keyboard Case ${i}`,
        email: `intl.${i}@innogen.com`,
        password: foreignPass,
        expectedStatus: 'success',
        description: 'Verify that multi-byte international character inputs are handled correctly on mobile.'
    });
}

// Whitespace variations (20 cases)
for (let i = 1; i <= 20; i++) {
    testCases.push({
        id: `APP-BDY-${String(edgeCount++).padStart(3, '0')}`,
        category: 'Boundary & Edge Cases',
        name: `Whitespace Padding Case ${i}`,
        email: ` user.space.${i}@innogen.com `,
        password: `Pass word ${i} !`,
        expectedStatus: 'success',
        description: 'Verify that trailing/leading whitespaces in email are trimmed on mobile.'
    });
}

module.exports = { testCases };
