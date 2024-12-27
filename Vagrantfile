Vagrant.configure("2") do |config|

    config.vm.define "linux" do |linux|
        linux.vm.box = "archlinux/archlinux"
        linux.vm.synced_folder ".", "/home/vagrant/sandpolis", type: "nfs", nfs_version: 4

        linux.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16
        end

        linux.vm.provider "libvirt" do |libvirt|
            libvirt.memory = 8192
            libvirt.cpus = 16
        end

        # Configure environment
        linux.vm.provision :shell, :inline => "hostnamectl set-hostname sandpolis_linux && locale-gen en_US.UTF.8"
        linux.vm.provision :shell, :inline => "pacman -Syu --noconfirm binutils gcc make git wget vim python-pip npm linux-headers"

        # Install Rust
        linux.vm.provision :shell, :inline => "curl https://sh.rustup.rs -sSf | sh -s -- -y"
    end

    config.vm.define "openbsd" do |openbsd|
        openbsd.vm.box = "poad/openbsd"
        openbsd.ssh.shell = "sh"
        openbsd.vm.synced_folder ".", "/home/vagrant/sandpolis", type: "nfs"

        # NFS needs host-only network
        openbsd.vm.network :private_network, ip: "172.16.2.42"

        openbsd.vm.provider "virtualbox" do |virtualbox|
            virtualbox.memory = 8192
            virtualbox.cpus = 16
        end
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
        windows.vm.provision :shell, :inline => "choco install -y windows-sdk-10.0"
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
