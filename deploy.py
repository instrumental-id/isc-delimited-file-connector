#!/usr/bin/env python3


# The JSON passed to create the connector
import argparse
import requests 
import sys
import os

# Where to find the config files
DEFAULT_CONFIG_PATH = "./config"

# The default type name of the connector (must match what's in connector.json)
DEFAULT_TYPE_NAME = "custom_delimitedFileDirect"

# The default locale for translations
DEFAULT_LOCALE = "en"

# File names for various parts of the connector
CONNECTOR_JSON_FILE = "connector.json"
SOURCE_TEMPLATE_FILE = "source-template.xml"
SOURCE_CONFIG_FILE = "source-config.xml"
EN_TRANSLATIONS_FILE = "translations.en.properties"

class DeployConfig:
    """
    Configuration for deploying the connector
    """

    def __init__(self, access_token, tenant, type_name="DelimitedFileDirectConnector", config_path="./config"):
        """Initialize the DeployConfig with necessary parameters"""
        self.access_token = access_token
        self.tenant = tenant
        self.connector_type = type_name
        self.config_path = config_path

        self.connectors_write_url = f"https://{tenant}/v2025/connectors"
        self.connectors_read_url = f"https://{tenant}/v2025/connectors"
        self.source_template_url = f"https://{tenant}/v2025/connectors/{{scriptName}}/source-template"
        self.translations_url = f"https://{tenant}/v2025/connectors/{{scriptName}}/translations/{{locale}}"
        self.source_config_url = f"https://{tenant}/v2025/connectors/{{scriptName}}/source-config"

    def set_script_name(self, scriptName):
        """Sets the script name in the URLs"""
        self.source_template_url = self.source_template_url.replace("{scriptName}", scriptName)
        self.translations_url = self.translations_url.replace("{scriptName}", scriptName)
        self.source_config_url = self.source_config_url.replace("{scriptName}", scriptName)

def deploy_connector(deploy_config: DeployConfig, json: str) -> str:
    """Deploys the connector and returns the script name"""
    url = deploy_config.connectors_write_url
    headers = {
        "Authorization": f"Bearer {deploy_config.access_token}",
        "Content-Type": "application/json"
    }
    print(f"Creating connector at {url}")
    response = requests.post(url, headers=headers, data=json)
    if response.status_code >= 200 and response.status_code < 300:
        data = response.json()
        script_name = data.get("scriptName")
        print(f"Connector created with scriptName: {script_name}")
        return script_name
    else:
        raise Exception(f"Failed to create connector. Status code: {response.status_code}, Response: {response.text}")

# find_connector searches for a connector matching the name and returns the script name if it exists
def find_connector_script_name(deploy_config: DeployConfig) -> str:
    """Finds a connector by type and returns its script name if it exists"""
    # URL is /connectors?filter=name eq 'connectorName'
    url = deploy_config.connectors_read_url + f"?filters=type eq \"{deploy_config.connector_type}\""

    headers = {
        "Authorization": f"Bearer {deploy_config.access_token}",
        "Content-Type": "application/json"
    }

    print(f"Searching for connector '{deploy_config.connector_type}' at {url}")
    response = requests.get(url, headers=headers)
    if response.status_code >= 200 and response.status_code < 300:
        # JSON list of connectors
        data = response.json()

        if (len(data) > 0):
            connector = data[0]
            script_name = connector.get("scriptName")
            print(f"Found existing connector with scriptName: {script_name}")
            return script_name
        else:
            print(f"No existing connector found with type = '{deploy_config.connector_type}'.")
    else:
        raise Exception(f"Failed to search for connector. Status code: {response.status_code}, Response: {response.text}")
    
    return ""

def deploy_source_template(deploy_config: DeployConfig, source_template_file: str):
    """Deploys the source template file"""
    # PUT a multipart form upload with the name 'file'
    url = deploy_config.source_template_url
    headers = {
        "Authorization": f"Bearer {deploy_config.access_token}",
        "Accept": "application/json"
    }

    with open(f"{deploy_config.config_path}/{source_template_file}", "rb") as file:
        print(f"Uploading source template to {url}")
        response = requests.put(url, headers=headers, files={"file": file})
        if response.status_code >= 200 and response.status_code < 300:
            print("Source template uploaded successfully.")
        else:
            raise Exception(f"Failed to upload source template. Status code: {response.status_code}, Response: {response.text}")

def deploy_source_config(deploy_config: DeployConfig, source_config_file: str):
    """Deploys the source config file"""
    # PUT a multipart form upload with the name 'file'
    url = deploy_config.source_config_url
    headers = {
        "Authorization": f"Bearer {deploy_config.access_token}",
        "Accept": "application/json"
    }
    with open(f"{deploy_config.config_path}/{source_config_file}", "rb") as file:
        print(f"Uploading source config to {url}")
        response = requests.put(url, headers=headers, files={"file": file})
        if response.status_code >= 200 and response.status_code < 300:
            print("Source config uploaded successfully.")
        else:
            raise Exception(f"Failed to upload source config. Status code: {response.status_code}, Response: {response.text}")
    
def deploy_translations(deploy_config: DeployConfig, translations_file: str, locale: str="en"):
    """
    Deploys the translations file
    
    :param deploy_config: DeployConfig object with deployment settings
    :param translations_file: Name of the translations file to upload
    :param locale: Locale code for the translations (default is "en")
    """
    # PUT a multipart form upload with the name 'object'
    url = deploy_config.translations_url.replace("{locale}", locale)

    headers = {
        "Authorization": f"Bearer {deploy_config.access_token}",
        "Accept": "application/json"
    }
    with open(f"{deploy_config.config_path}/{translations_file}", "rb") as file:
        print(f"Uploading translations to {url}")
        response = requests.put(url, headers=headers, files={"object": file})
        if response.status_code >= 200 and response.status_code < 300:
            print("Translations uploaded successfully.")
        else:
            raise Exception(f"Failed to upload translations. Status code: {response.status_code}, Response: {response.text}")
        
def parse_args() -> DeployConfig:
    parser = argparse.ArgumentParser(description="Deploy the DelimitedFileDirectConnector to SailPoint.")
    parser.add_argument("--token", help="Access token for authentication")
    parser.add_argument("--tenant", help="Tenant domain (e.g. example.identitynow.com)")
    parser.add_argument("--config", help="Path to the connector config directory", default="./config")
    args = parser.parse_args()

    access_token = ""
    tenant = ""
    config_path = DEFAULT_CONFIG_PATH

    if os.path.exists(".env"):
        with open(".env", "r") as f:
            for line in f:
                # Skip lines that are comments or empty or don't contain '='
                if line.startswith("#") or "=" not in line:
                    continue
                key, value = line.strip().split("=", 1)
                if key == "ACCESS_TOKEN":
                    access_token = value
                elif key == "TENANT":
                    tenant = value
                elif key == "CONFIG_PATH":
                    config_path = value

    # Override .env values with command line arguments if provided
    if args.token: 
        access_token = args.token
    
    if args.tenant:
        tenant = args.tenant

    if args.config:
        config_path = args.config

    if not access_token or not tenant:
        raise Exception("Access token and tenant must be provided either via .env file or command line arguments.")
    
    # If config does not exist, error
    if not os.path.exists(config_path):
        raise Exception(f"Config path '{config_path}' does not exist.")

    config = DeployConfig(access_token, tenant, DEFAULT_TYPE_NAME, config_path)

    return config

def main():
    """
    Main deployment function. 

    To deploy, we need to:
        1a. Check to see if the connector already exists (GET to /connectors/{scriptName})
        1b. If not, create the connector (POST to /connectors). Result will contain the scriptName.
        2. Upload the source template (PUT to /connectors/{scriptName}/source-template)
        3. Upload translations (PUT to /connectors/{scriptName}/translations/{locale})
        4. Upload source config (PUT to /connectors/{scriptName}/source-config

    :return: None
    """
    config = parse_args()

    try:

        connector_json_file = f"{config.config_path}/{CONNECTOR_JSON_FILE}"
        with open(connector_json_file, "r") as f:
            connector_json = f.read()
        
        script_name = find_connector_script_name(config)
        if not script_name:
            script_name = deploy_connector(config, connector_json)

        print (f"Using scriptName: {script_name}")

        config.set_script_name(script_name)

        deploy_source_template(config, SOURCE_TEMPLATE_FILE)

        # It may make sense to support more than one locale in the future
        deploy_translations(config, EN_TRANSLATIONS_FILE, DEFAULT_LOCALE)

        deploy_source_config(config, SOURCE_CONFIG_FILE)
    except Exception as e:
        print(f"Error during deployment: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
