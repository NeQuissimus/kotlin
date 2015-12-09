/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler.classFile

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProviderBase

public class KotlinClassFileViewProvider(
        manager: PsiManager,
        file: VirtualFile,
        physical: Boolean,
        val isInternal: Boolean) : KotlinDecompiledFileViewProviderBase(manager, file, physical) {

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        val fileIndex = ServiceManager.getService(project, javaClass<FileIndexFacade>())
        if (!fileIndex.isInLibraryClasses(file) && fileIndex.isInSource(file)) {
            return null
        }

        if (isInternal) return null

        return KtClsFile(this)
    }

    override fun createCopy(copy: VirtualFile) = KotlinClassFileViewProvider(getManager(), copy, false, isInternal)
}