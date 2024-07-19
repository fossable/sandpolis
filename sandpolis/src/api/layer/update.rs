//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
syntax = "proto3";

package plugin.update;

option java_package = "org.s7s.plugin.update";

message RQ_InstallOrUpgradePackages {
    repeated string package = 1;
}

message RQ_RemovePackages {
    repeated string package = 1;
}

message RQ_RefreshPackages {
}
