package com.yiistorm.helpers;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PhpElementsUtil {
    static public List<ResolveResult> getClassInterfaceResolveResult(Project project, String FQNClassOrInterfaceName) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(FQNClassOrInterfaceName);
        Collection<PhpClass> phpInterfaces = phpIndex.getInterfacesByFQN(FQNClassOrInterfaceName);

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }
        for (PhpClass phpInterface : phpInterfaces) {
            results.add(new PsiElementResolveResult(phpInterface));
        }

        return results;
    }

    static public PsiElement[] getClassInterfacePsiElements(Project project, String FQNClassOrInterfaceName) {

        // convert ResolveResult to PsiElement
        List<PsiElement> results = new ArrayList<PsiElement>();
        for (ResolveResult result : getClassInterfaceResolveResult(project, FQNClassOrInterfaceName)) {
            results.add(result.getElement());
        }

        return results.toArray(new PsiElement[results.size()]);
    }

    static public boolean isMethodWithFirstString(PsiElement psiElement, String... methodName) {

        // filter out method calls without parameter
        // $this->methodName('service_name')
        // withName is not working, so simulate it in a hack
        if (!PlatformPatterns
                .psiElement(PhpElementTypes.METHOD_REFERENCE)
                .withChild(PlatformPatterns
                        .psiElement(PhpElementTypes.PARAMETER_LIST)
                        .withFirstChild(PlatformPatterns
                                .psiElement(PhpElementTypes.STRING)
                        )
                ).accepts(psiElement)) {

            return false;
        }

        // cant we move it up to PlatformPatterns? withName condition dont looks working
        String methodRefName = ((MethodReference) psiElement).getName();

        return null != methodRefName && Arrays.asList(methodName).contains(methodRefName);
    }

    static public PsiElementPattern.Capture<StringLiteralExpression> methodWithFirstStringPattern() {
        return PlatformPatterns
                .psiElement(StringLiteralExpression.class)
                .withParent(
                        PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                                .withFirstChild(
                                        PlatformPatterns.psiElement(PhpElementTypes.STRING)
                                )
                                .withParent(
                                        PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE)
                                )
                )
                .withLanguage(PhpLanguage.INSTANCE);
    }

    @Nullable
    static public PhpClass getClass(PhpIndex phpIndex, String className) {
        Collection<PhpClass> classes = phpIndex.getClassesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    static public PhpClass getInterface(PhpIndex phpIndex, String className) {
        Collection<PhpClass> classes = phpIndex.getInterfacesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    static public void addClassPublicMethodCompletion(CompletionResultSet completionResultSet, PhpClass phpClass) {
        for (Method method : phpClass.getMethods()) {
            if (method.getAccess().isPublic() && !method.getName().startsWith("__")) {
                completionResultSet.addElement(new PhpLookupElement(method));
            }
        }
    }

    @Nullable
    static public String getArrayHashValue(ArrayCreationExpression arrayCreationExpression, String keyName) {
        ArrayHashElement translationArrayHashElement = PsiElementUtils.getChildrenOfType(arrayCreationExpression, PlatformPatterns.psiElement(ArrayHashElement.class)
                .withFirstChild(
                        PlatformPatterns.psiElement(PhpElementTypes.ARRAY_KEY).withText(
                                PlatformPatterns.string().oneOf("'" + keyName + "'", "\"" + keyName + "\"")
                        )
                )
        );

        if (translationArrayHashElement == null) {
            return null;
        }

        if (!(translationArrayHashElement.getValue() instanceof StringLiteralExpression)) {
            return null;
        }

        StringLiteralExpression valueString = (StringLiteralExpression) translationArrayHashElement.getValue();
        if (valueString == null) {
            return null;
        }

        return valueString.getContents();

    }

}
