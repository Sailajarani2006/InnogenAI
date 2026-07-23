const { expect } = require('expect-webdriverio');
const { testCases } = require('./test-cases.js');

describe('InnoGenAI Mobile Appium Login E2E Suite', () => {
    
    before(async () => {
        console.log('[Appium] Launching Jetpack Compose app session...');
    });

    testCases.forEach((tc) => {
        it(`[${tc.id}] ${tc.name}`, async () => {
            // Find OutlinedTextFields by label text (standard Compose translation in UI Automator)
            const emailField = await $('android=new UiSelector().className("android.widget.EditText").text("Email Address")');
            const passwordField = await $('android=new UiSelector().className("android.widget.EditText").text("Password")');
            const signInBtn = await $('android=new UiSelector().className("android.widget.Button").text("Sign In")');

            // Enter credentials
            await emailField.setValue(tc.email);
            await passwordField.setValue(tc.password);

            // Submit Form
            await signInBtn.click();

            // Validation outcome checks
            if (tc.expectedStatus === 'success') {
                // Check dashboard loads
                const dashboardHeader = await $('android=new UiSelector().textContains("Dashboard")');
                await expect(dashboardHeader).toBeDisplayed();
                
                // Sign out to clean state for next test
                const profileTab = await $('android=new UiSelector().text("Profile")');
                await profileTab.click();
                const signOutBtn = await $('android=new UiSelector().text("Sign Out")');
                await signOutBtn.click();
            } else {
                // Error message visual check
                const errorContainer = await $('android=new UiSelector().textContains("Validation failed")');
                await expect(errorContainer).toBeDisplayed();
            }
        });
    });
});
