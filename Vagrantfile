Vagrant.configure("2") do |config|

    config.vm.define "linux" do |linux|
        linux.vm.box = "archlinux/archlinux"
        linux.vm.synced_folder ".", "/home/vagrant/sandpolis"

        linux.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16
        end

        # Configure environment
        linux.vm.provision :shell, :inline => "hostnamectl set-hostname sandpolis_linux && locale-gen en_US.UTF.8"
        linux.vm.provision :shell, :inline => "pacman -Syu --noconfirm binutils gcc make git wget vim python-pip npm linux-headers"

        # Install Java
        linux.vm.provision :shell, :inline => "wget -q -O- https://download.java.net/java/early_access/panama/3/openjdk-17-panama+3-167_linux-x64_bin.tar.gz | tar zxf -"
        linux.vm.provision :shell, :inline => "echo 'export JAVA_HOME=/home/vagrant/jdk-17' >>/home/vagrant/.bash_profile"

        # Install Rust
        linux.vm.provision :shell, :inline => "curl https://sh.rustup.rs -sSf | sh -s -- -y"

        # Install Swift
        linux.vm.provision :shell, :inline => "wget -q -O- https://swift.org/builds/swift-5.4.3-release/ubuntu2004/swift-5.4.3-RELEASE/swift-5.4.3-RELEASE-ubuntu20.04.tar.gz | tar zxf -"
        linux.vm.provision :shell, :inline => "echo 'export PATH=\${PATH}:~/swift-5.4.3-RELEASE-ubuntu20.04/usr/bin' >>/home/vagrant/.bash_profile"
        linux.vm.provision :shell, :inline => "source /home/vagrant/.bash_profile"

        # Install protoc-gen-swift
        #linux.vm.provision :shell, :inline => "git clone --depth 1 https://github.com/apple/swift-protobuf"
        #linux.vm.provision :shell, :inline => "(cd swift-protobuf; swift build -c release; cp .build/release/protoc-gen-swift /usr/bin/protoc-gen-swift)"

        # Install protoc-gen-rust
        linux.vm.provision :shell, :inline => "git clone --depth 1 --branch v2.25 https://github.com/stepancheg/rust-protobuf"
        linux.vm.provision :shell, :inline => "(cd rust-protobuf; cargo build --package protobuf-codegen --release; cp target/release/protoc-gen-rust /usr/bin/protoc-gen-rust)"

        # Install formatters
        linux.vm.provision :shell, :inline => "pacman -Syu --noconfirm python-black"
        linux.vm.provision :shell, :inline => "npm install -g prettier"
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
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/efi/0/Config/DmiSystemProduct", "MacBookPro11,3"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/efi/0/Config/DmiSystemVersion", "1.0"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/efi/0/Config/DmiBoardProduct", "Iloveapple"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/smc/0/Config/DeviceKey", "ourhardworkbythesewordsguardedpleasedontsteal(c)AppleComputerInc"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal/Devices/smc/0/Config/GetKeyFromRealSMC", "1"]
            virtualbox.customize ["setextradata", :id, "VBoxInternal2/EfiGopMode", "4"]
        end

        macos.vm.provision :shell, :inline => "brew install --cask adoptopenjdk"

        # Install Xcode
        #macos.vm.provision :shell, :inline => "gem install xcode-install"
        #macos.vm.provision :shell, :inline => "xcversion install"
    end
end
