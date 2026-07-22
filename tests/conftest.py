import pytest
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
import os

@pytest.fixture(scope="session")
def browser():
    """
    Setup Selenium WebDriver for Chrome in headless mode.
    Ideal for GitHub Actions CI/CD environments.
    """
    chrome_options = Options()
    chrome_options.add_argument("--headless=new")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--window-size=1920,1080")

    # Initialize WebDriver
    driver = webdriver.Chrome(options=chrome_options)
    
    # Implicit wait for elements to appear
    driver.implicitly_wait(10)

    yield driver
    
    # Teardown
    driver.quit()

@pytest.fixture(scope="session")
def base_url():
    """
    Returns the base URL for the tests.
    In CI, this points to a local static server serving the 'dist' folder.
    """
    return os.getenv("TEST_URL", "http://127.0.0.1:8080")
