/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.inspections.quickfix

import com.intellij.codeInsight.AnnotationTargetUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.ExternalAnnotationsManager.AnnotationPlace.*
import com.intellij.codeInsight.daemon.impl.analysis.AnnotationsHighlightUtil
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.util.IntentionName
import com.intellij.java.analysis.JavaAnalysisBundle
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.util.JavaElementKind
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ObjectUtils
import com.siyeh.ig.psiutils.CommentTracker
import one.util.streamex.StreamEx
import java.lang.annotation.RetentionPolicy

class RewriteAnnotationQuickFix(
    private val myAnnotation: String,
    modifierListOwner: PsiModifierListOwner,
    values: Array<PsiNameValuePair>,
    place: ExternalAnnotationsManager.AnnotationPlace,
    vararg annotationsToRemove: String
) : LocalQuickFixOnPsiElement(modifierListOwner), LocalQuickFix, IntentionAction {
    private val annotationsToRemove: Array<String>

    @FileModifier.SafeFieldForPreview
    val myPairs: Array<PsiNameValuePair>
    private var myText: @IntentionName String
    private val myAnnotationPlace: ExternalAnnotationsManager.AnnotationPlace
    private val myExistsTypeUseTarget: Boolean
    private val myAvailableInBatchMode: Boolean

    constructor(
        fqn: String, modifierListOwner: PsiModifierListOwner, vararg annotationsToRemove: String
    ) : this(fqn, modifierListOwner, PsiNameValuePair.EMPTY_ARRAY, *annotationsToRemove)

    constructor(
        fqn: String,
        modifierListOwner: PsiModifierListOwner,
        values: Array<PsiNameValuePair>,
        vararg annotationsToRemove: String
    ) : this(fqn, modifierListOwner, values, choosePlace(fqn, modifierListOwner), *annotationsToRemove)

    init {
        ObjectUtils.assertAllElementsNotNull(values)
        myPairs = values
        ObjectUtils.assertAllElementsNotNull(annotationsToRemove)
        this.annotationsToRemove = arrayOf(*annotationsToRemove)
        myText = calcText(modifierListOwner, myAnnotation)
        myAnnotationPlace = place
        myAvailableInBatchMode = place == IN_CODE || place == EXTERNAL && ExternalAnnotationsManager.getInstance(
            modifierListOwner.project
        ).hasConfiguredAnnotationRoot(modifierListOwner)

        val annotationClass =
            JavaPsiFacade.getInstance(modifierListOwner.project).findClass(myAnnotation, modifierListOwner.resolveScope)
        myExistsTypeUseTarget = annotationClass != null && AnnotationTargetUtil.findAnnotationTarget(
            annotationClass, PsiAnnotation.TargetType.TYPE_USE
        ) != null
    }

    override fun getText(): String {
        return myText
    }

    override fun getFamilyName(): String {
        return JavaAnalysisBundle.message("intention.add.annotation.family")
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(file)) {
            val psiElement = startElement
            if (psiElement == null || psiElement.containingFile !== file) return false
        }
        return isAvailable
    }

    override fun isAvailable(
        project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement
    ): Boolean {
        val modifierListOwner = startElement as? PsiModifierListOwner ?: return false

        if (!modifierListOwner.isValid) return false
        if (!PsiUtil.isLanguageLevel5OrHigher(modifierListOwner)) return false

        if (modifierListOwner is PsiParameter && modifierListOwner.typeElement == null) {
            if (modifierListOwner.getParent() is PsiParameterList) {
                val lambda = modifierListOwner.getParent().parent as? PsiLambdaExpression ?: return false

                // Lambda parameter without type cannot be annotated. Check if we can specify types
                if (PsiUtil.isLanguageLevel11OrHigher(modifierListOwner)) return true
                return LambdaUtil.createLambdaParameterListWithFormalTypes(
                    lambda.functionalInterfaceType, lambda, false
                ) != null
            }
            return false
        }

        // e.g. PsiTypeParameterImpl doesn't have modifier list
        val modifierList = modifierListOwner.modifierList
        return (modifierList != null && modifierList !is LightElement && modifierListOwner !is LightElement)
    }

    override fun startInWriteAction(): Boolean {
        return myAnnotationPlace == IN_CODE
    }

    override fun availableInBatchMode(): Boolean {
        return myAvailableInBatchMode
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        applyFix()
    }

    override fun invoke(
        project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement
    ) {
        val modifierListOwner = startElement as PsiModifierListOwner
        val target = AnnotationTargetUtil.getTarget(modifierListOwner, myExistsTypeUseTarget) ?: return
        val annotationsManager = ExternalAnnotationsManager.getInstance(project)
        val place = if (myAnnotationPlace == NEED_ASK_USER) annotationsManager.chooseAnnotationsPlace(
            modifierListOwner
        ) else myAnnotationPlace
        when (place) {
            NOWHERE, NEED_ASK_USER -> {
            }

            EXTERNAL -> {
                for (fqn in annotationsToRemove) {
                    annotationsManager.deannotate(modifierListOwner, fqn)
                }
                try {
                    annotationsManager.annotateExternally(modifierListOwner, myAnnotation, file, myPairs)
                } catch (ignored: ExternalAnnotationsManager.CanceledConfigurationException) {
                }
            }

            IN_CODE -> {
                val containingFile = modifierListOwner.containingFile
                val command = Runnable {
                    removePhysicalAnnotations(
                        modifierListOwner, *annotationsToRemove
                    )
                    val inserted = addPhysicalAnnotationTo(
                        addAnnotation(
                            target, myAnnotation
                        ), myPairs
                    )
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(inserted)
                }

                if (!containingFile.isPhysical) {
                    command.run()
                } else {
                    WriteCommandAction.runWriteCommandAction(project, null, null, command, containingFile)
                }

                if (containingFile !== file) {
                    UndoUtil.markPsiFileForUndo(file)
                }
            }
        }
    }

    private fun addAnnotation(annotationOwner: PsiAnnotationOwner, fqn: String): PsiAnnotation {
        return expandParameterAndAddAnnotation(annotationOwner, fqn)
    }

    companion object {
        fun calcText(modifierListOwner: PsiModifierListOwner?, annotation: String?): @IntentionName String {
            val shortName = annotation?.substring(annotation.lastIndexOf('.') + 1)
            if (modifierListOwner is PsiNamedElement) {
                val name = PsiFormatUtil.formatSimple(modifierListOwner)
                if (name != null) {
                    val type = JavaElementKind.fromElement(modifierListOwner).lessDescriptive()
                    if (shortName == null) {
                        return JavaAnalysisBundle.message(
                            "inspection.i18n.quickfix.annotate.element", type.`object`(), name
                        )
                    }
                    return JavaAnalysisBundle.message(
                        "inspection.i18n.quickfix.annotate.element.as", type.`object`(), name, shortName
                    )
                }
            }
            if (shortName == null) {
                return JavaAnalysisBundle.message("inspection.i18n.quickfix.annotate")
            }
            return JavaAnalysisBundle.message("inspection.i18n.quickfix.annotate.as", shortName)
        }

        fun choosePlace(
            annotation: String, modifierListOwner: PsiModifierListOwner
        ): ExternalAnnotationsManager.AnnotationPlace {
            val project = modifierListOwner.project
            val annotationsManager = ExternalAnnotationsManager.getInstance(project)
            if (BaseIntentionAction.canModify(modifierListOwner)) {
                val aClass = JavaPsiFacade.getInstance(project).findClass(annotation, modifierListOwner.resolveScope)
                if (aClass != null) {
                    if (AnnotationsHighlightUtil.getRetentionPolicy(aClass) == RetentionPolicy.RUNTIME) {
                        return IN_CODE
                    }
                    if (CommonClassNames.DEFAULT_PACKAGE != StringUtil.getPackageName(annotation)) {
                        val resolvedBySimpleName =
                            JavaPsiFacade.getInstance(project).resolveHelper.resolveReferencedClass(
                                StringUtil.getShortName(annotation), modifierListOwner
                            )
                        if (resolvedBySimpleName != null && resolvedBySimpleName.manager.areElementsEquivalent(
                                resolvedBySimpleName, aClass
                            )
                        ) {
                            // if class is already imported in current file
                            return IN_CODE
                        }
                    }
                }
            }
            return annotationsManager.chooseAnnotationsPlaceNoUi(modifierListOwner)
        }

        @Suppress("unused") // for backwards compatibility
        fun addPhysicalAnnotationTo(
            fqn: String, pairs: Array<PsiNameValuePair>, owner: PsiAnnotationOwner
        ): PsiAnnotation {
            return addPhysicalAnnotationTo(expandParameterAndAddAnnotation(owner, fqn), pairs)
        }

        fun addPhysicalAnnotationTo(inserted: PsiAnnotation, pairs: Array<PsiNameValuePair>): PsiAnnotation {
            for (pair in pairs) {
                inserted.setDeclaredAttributeValue(pair.name, pair.value)
            }
            return inserted
        }

        fun expandParameterAndAddAnnotation(annotationOwner: PsiAnnotationOwner, fqn: String): PsiAnnotation {
            return (if (annotationOwner is PsiModifierList) {
                expandParameterIfNecessary(annotationOwner)
            } else {
                annotationOwner
            }).addAnnotation(fqn)
        }

        private fun expandParameterIfNecessary(owner: PsiModifierList): PsiModifierList {
            var result = owner
            var parameter = ObjectUtils.tryCast(
                result.parent, PsiParameter::class.java
            )
            if (parameter != null && parameter.typeElement == null) {
                var list = ObjectUtils.tryCast(
                    parameter.parent, PsiParameterList::class.java
                )
                if (list != null && list.parent is PsiLambdaExpression) {
                    val parameters = list.parameters
                    val index = ArrayUtil.indexOf(parameters, parameter)
                    var newList: PsiParameterList?
                    if (PsiUtil.isLanguageLevel11OrHigher(list)) {
                        val newListText =
                            StreamEx.of(*parameters).map { p: PsiParameter -> PsiKeyword.VAR + " " + p.name }
                                .joining(",", "(", ")")
                        newList = (JavaPsiFacade.getElementFactory(list.project)
                            .createExpressionFromText("$newListText -> {}", null) as PsiLambdaExpression).parameterList
                        newList = CommentTracker().replaceAndRestoreComments(list, newList) as PsiParameterList
                    } else {
                        newList = LambdaUtil.specifyLambdaParameterTypes(list.parent as PsiLambdaExpression)
                    }
                    if (newList != null) {
                        list = newList
                        parameter = list.getParameter(index)
                        LOG.assertTrue(parameter != null)
                        result = parameter!!.modifierList!!
                    }
                }
            }
            return result
        }

        fun removePhysicalAnnotations(owner: PsiModifierListOwner, vararg fqns: String) {
            for (fqn in fqns) {
                val annotation = AnnotationUtil.findAnnotation(owner, true, fqn)
                if (annotation != null && !AnnotationUtil.isInferredAnnotation(annotation)) {
                    CommentTracker().deleteAndRestoreComments(annotation)
                }
            }
        }

    }
}
