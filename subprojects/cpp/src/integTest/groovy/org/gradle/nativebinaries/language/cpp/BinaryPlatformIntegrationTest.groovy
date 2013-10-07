/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativebinaries.language.cpp
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativebinaries.language.cpp.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativebinaries.language.cpp.fixtures.ExecutableFixture
import org.gradle.nativebinaries.language.cpp.fixtures.app.CppHelloWorldApp
import org.gradle.nativebinaries.language.cpp.fixtures.binaryinfo.DumpbinBinaryInfo
import org.gradle.nativebinaries.language.cpp.fixtures.binaryinfo.OtoolBinaryInfo
import org.gradle.nativebinaries.language.cpp.fixtures.binaryinfo.ReadelfBinaryInfo
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

class BinaryPlatformIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {
    def helloWorldApp = new CppHelloWorldApp()

    def setup() {
        buildFile << """
            apply plugin: 'cpp'

            executables {
                main {}
            }
            libraries {
                hello {}
            }
            sources.main.cpp.lib libraries.hello
        """

        helloWorldApp.executable.writeSources(file("src/main"))
        helloWorldApp.library.writeSources(file("src/hello"))
    }

    @Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
    def "build binary for multiple target architectures"() {
        // Don't yet have test environments to build 64-bit binaries on MinGW or cygwin
        if (OperatingSystem.current().windows && !toolChain.visualCpp) {
            return
        }

        when:
        buildFile << """
            targetPlatforms {
                x86 {
                    architecture "x86"
                }
                x86_64 {
                    architecture "x86_64"
                }
            }
        """

        and:
        executer.withArgument("--debug")
        succeeds "installX86MainExecutable", "installX86_64MainExecutable"

        then:
        binaryInfo(executable("build/binaries/mainExecutable/x86/main").file).arch.name == "x86"
        binaryInfo(objectFile("build/objectFiles/mainExecutable/x86/mainCpp/main")).arch.name == "x86"

        binaryInfo(executable("build/binaries/mainExecutable/x86_64/main").file).arch.name == "x86_64"
        binaryInfo(objectFile("build/objectFiles/mainExecutable/x86_64/mainCpp/main")).arch.name == "x86_64"
    }

    @Requires(TestPrecondition.WINDOWS)
    def "build binary for itanium architecture"() {
        // Don't yet have test environments to build Itanium binaries on MinGW or cygwin
        if (!toolChain.visualCpp) {
            return
        }

        when:
        buildFile << """
            targetPlatforms {
                itanium {
                    architecture "ia-64"
                }
            }
        """

        and:
        executer.withArgument("--debug")
        succeeds "installMainExecutable"

        then:
        binaryInfo(executable("build/binaries/mainExecutable/main").file).arch.name == "ia-64"
        binaryInfo(objectFile("build/objectFiles/mainExecutable/mainCpp/main")).arch.name == "ia-64"
    }

    @Requires(TestPrecondition.CAN_INSTALL_EXECUTABLE)
    def "can configure binary for multiple target operating systems"() {
        when:
        buildFile << """
            targetPlatforms {
                solaris {
                    operatingSystem "solaris"
                }
                windows {
                    operatingSystem "windows"
                }
            }

            binaries.matching({ it.targetPlatform.operatingSystem.windows }).all {
                cppCompiler.define "FRENCH"
            }
        """

        and:
        succeeds "installSolarisMainExecutable", "installWindowsMainExecutable"

        then:
        installation("build/install/mainExecutable/solaris").exec().out ==  helloWorldApp.englishOutput
        installation("build/install/mainExecutable/windows").exec().out ==  helloWorldApp.frenchOutput
    }

    def binaryInfo(TestFile file) {
        file.assertIsFile()
        if (OperatingSystem.current().isMacOsX()) {
            return new OtoolBinaryInfo(file)
        }
        if (OperatingSystem.current().isWindows()) {
            return new DumpbinBinaryInfo(file, toolChain)
        }
        return new ReadelfBinaryInfo(file)
    }

    def ExecutableFixture executable(Object path) {
        return toolChain.executable(file(path))
    }

    def TestFile objectFile(Object path) {
        return toolChain.objectFile(file(path));
    }

}
