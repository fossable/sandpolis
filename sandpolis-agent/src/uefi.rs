use anyhow::{Context, Result};
use std::fs;
use std::path::PathBuf;

const EFIVARS_PATH: &str = "/sys/firmware/efi/efivars";

// EFI Global Variable GUID
const EFI_GLOBAL_VARIABLE_GUID: &str = "8be4df61-93ca-11d2-aa0d-00e098032b8c";

/// Common UEFI variables
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum UefiVariable {
    /// Boot order list (BootOrder)
    BootOrder,
    /// Boot entry (Boot####)
    BootEntry(u16),
    /// Boot current (BootCurrent)
    BootCurrent,
    /// Boot next (BootNext)
    BootNext,
    /// Boot option support (BootOptionSupport)
    BootOptionSupport,
    /// Timeout (Timeout)
    Timeout,
    /// Secure boot enable (SecureBoot)
    SecureBoot,
    /// Setup mode (SetupMode)
    SetupMode,
    /// Platform key (PK)
    PlatformKey,
    /// Key exchange key (KEK)
    KeyExchangeKey,
    /// Authorized signature database (db)
    SignatureDatabase,
    /// Forbidden signature database (dbx)
    ForbiddenSignatureDatabase,
    /// Custom variable with name and GUID
    Other { name: String, vendor_guid: String },
}

impl UefiVariable {
    /// Get the variable name
    fn name(&self) -> String {
        match self {
            UefiVariable::BootOrder => "BootOrder".to_string(),
            UefiVariable::BootEntry(num) => format!("Boot{:04X}", num),
            UefiVariable::BootCurrent => "BootCurrent".to_string(),
            UefiVariable::BootNext => "BootNext".to_string(),
            UefiVariable::BootOptionSupport => "BootOptionSupport".to_string(),
            UefiVariable::Timeout => "Timeout".to_string(),
            UefiVariable::SecureBoot => "SecureBoot".to_string(),
            UefiVariable::SetupMode => "SetupMode".to_string(),
            UefiVariable::PlatformKey => "PK".to_string(),
            UefiVariable::KeyExchangeKey => "KEK".to_string(),
            UefiVariable::SignatureDatabase => "db".to_string(),
            UefiVariable::ForbiddenSignatureDatabase => "dbx".to_string(),
            UefiVariable::Other { name, .. } => name.clone(),
        }
    }

    /// Get the vendor GUID
    fn vendor_guid(&self) -> String {
        match self {
            UefiVariable::Other { vendor_guid, .. } => vendor_guid.clone(),
            _ => EFI_GLOBAL_VARIABLE_GUID.to_string(),
        }
    }

    /// Read the variable value and attributes
    ///
    /// # Returns
    /// A tuple of (data, attributes) where:
    /// * `data` - The variable data as a byte vector
    /// * `attributes` - The variable attributes as u32
    pub fn get(&self) -> Result<(Vec<u8>, u32)> {
        let var_path = PathBuf::from(EFIVARS_PATH).join(format!("{}-{}", self.name(), self.vendor_guid()));

        let raw_data = fs::read(&var_path)
            .with_context(|| format!("Failed to read UEFI variable: {}", var_path.display()))?;

        // UEFI variables in sysfs are stored with a 4-byte attributes header
        if raw_data.len() < 4 {
            anyhow::bail!("Invalid UEFI variable data: too short");
        }

        let attributes = u32::from_le_bytes([raw_data[0], raw_data[1], raw_data[2], raw_data[3]]);
        let data = raw_data[4..].to_vec();

        Ok((data, attributes))
    }

    /// Write the variable value with attributes
    ///
    /// # Arguments
    /// * `attributes` - The variable attributes as u32
    /// * `data` - The variable data as a byte slice
    ///
    /// # Note
    /// This requires root privileges and the efivarfs to be mounted read-write.
    pub fn set(&self, attributes: u32, data: &[u8]) -> Result<()> {
        let var_path = PathBuf::from(EFIVARS_PATH).join(format!("{}-{}", self.name(), self.vendor_guid()));

        // Construct the data with attributes header
        let mut raw_data = Vec::with_capacity(4 + data.len());
        raw_data.extend_from_slice(&attributes.to_le_bytes());
        raw_data.extend_from_slice(data);

        fs::write(&var_path, &raw_data)
            .with_context(|| format!("Failed to write UEFI variable: {}", var_path.display()))?;

        Ok(())
    }

    /// List all available UEFI variables.
    ///
    /// # Returns
    /// A vector of UefiVariable instances for each available variable.
    pub fn list() -> Result<Vec<UefiVariable>> {
        let entries = fs::read_dir(EFIVARS_PATH)
            .context("Failed to read efivars directory")?;

        let mut variables = Vec::new();

        for entry in entries {
            let entry = entry?;
            let filename = entry.file_name();
            let filename_str = filename.to_string_lossy();

            // Parse filename format: "VariableName-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            if let Some(dash_pos) = filename_str.rfind('-') {
                if dash_pos >= 36 {
                    let guid_start = dash_pos - 35;
                    let name = filename_str[..guid_start - 1].to_string();
                    let guid = filename_str[guid_start..].to_string();

                    // Try to match known variables
                    let var = if guid == EFI_GLOBAL_VARIABLE_GUID {
                        match name.as_str() {
                            "BootOrder" => UefiVariable::BootOrder,
                            "BootCurrent" => UefiVariable::BootCurrent,
                            "BootNext" => UefiVariable::BootNext,
                            "BootOptionSupport" => UefiVariable::BootOptionSupport,
                            "Timeout" => UefiVariable::Timeout,
                            "SecureBoot" => UefiVariable::SecureBoot,
                            "SetupMode" => UefiVariable::SetupMode,
                            "PK" => UefiVariable::PlatformKey,
                            "KEK" => UefiVariable::KeyExchangeKey,
                            "db" => UefiVariable::SignatureDatabase,
                            "dbx" => UefiVariable::ForbiddenSignatureDatabase,
                            n if n.starts_with("Boot") && n.len() == 8 => {
                                if let Ok(num) = u16::from_str_radix(&n[4..], 16) {
                                    UefiVariable::BootEntry(num)
                                } else {
                                    UefiVariable::Other { name, vendor_guid: guid }
                                }
                            }
                            _ => UefiVariable::Other { name, vendor_guid: guid },
                        }
                    } else {
                        UefiVariable::Other { name, vendor_guid: guid }
                    };

                    variables.push(var);
                }
            }
        }

        Ok(variables)
    }
}
