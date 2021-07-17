Vagrant.configure("2") do |config|

    config.vm.define "linux" do |linux|
        linux.vm.box = "ubuntu/groovy64"
        linux.vm.synced_folder ".", "/home/vagrant/sandpolis"

        linux.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16
        end

        # Configure environment
        linux.vm.provision :shell, :inline => "touch .hushlogin"
        linux.vm.provision :shell, :inline => "hostnamectl set-hostname sandpolis_linux && locale-gen en_US.UTF.8"
        linux.vm.provision :shell, :inline => "apt-get update --fix-missing"
        linux.vm.provision :shell, :inline => "apt-get install -q -y g++ make git curl vim libncursesw5-dev"

        # Configure Java
        linux.vm.provision :shell, :inline => "wget -q -O- https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.1+9/OpenJDK15U-jdk_x64_linux_hotspot_15.0.1_9.tar.gz | tar zxf -"
        linux.vm.provision :shell, :inline => "echo 'export JAVA_HOME=/home/vagrant/jdk-15.0.1+9' >>/home/vagrant/.profile"

        # Configure Rust
        linux.vm.provision :shell, :inline => "curl https://sh.rustup.rs -sSf | sh -s -- -y"

        # Configure rust-protobuf
        linux.vm.provision :shell, :inline => "git clone --depth 1 --branch v2.22 https://github.com/stepancheg/rust-protobuf"
        linux.vm.provision :shell, :inline => "(cd rust-protobuf; cargo build --package protobuf-codegen --release; cp target/release/protoc-gen-rust /usr/bin/protoc-gen-rust)"
    end

    config.vm.define "windows" do |windows|
        windows.vm.box = "gusztavvargadr/windows-10"
        windows.vm.synced_folder ".", "C:\\Users\\vagrant\\sandpolis"

        windows.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16
        end

        # Configure environment
        windows.vm.provision :shell, :inline => "choco install -y adoptopenjdk"
    end

    config.vm.define "macos" do |macos|
        macos.vm.box = "yzgyyang/macOS-10.14"

        macos.vm.synced_folder ".", "/vagrant",
            id: "core",
            :nfs => true,
            :mount_options => ['nolock,vers=3,udp,noatime,actimeo=1,resvport'],
        :export_options => ['async,insecure,no_subtree_check,no_acl,no_root_squash']

         # NFS needs host-only network
        macos.vm.network :private_network, ip: "172.16.2.42"

        macos.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16

            virtualbox.customize ["modifyvm", :id, "--cpuid-set", "00000001", "000106e5", "00100800", "0098e3fd", "bfebfbff"]
            virtualbox.customize ["modifyvm", :id, "--usb", "off"]
            virtualbox.customize ["modifyvm", :id, "--usbehci", "off"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/efi/0/Config/DmiSystemProduct", "MacBookPro11,3"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/efi/0/Config/DmiSystemVersion", "1.0"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/efi/0/Config/DmiBoardProduct", "Iloveapple"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/smc/0/Config/DeviceKey", "ourhardworkbythesewordsguardedpleasedontsteal(c)AppleComputerInc"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/smc/0/Config/GetKeyFromRealSMC", "1"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal2/EfiGopMode", "4"]
        end

        macos.vm.provision :shell, :inline => "brew install --cask adoptopenjdk"
    end
end
