/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java.abi.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.testutil.CompilerTreeApiParameterized;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;

@RunWith(CompilerTreeApiParameterized.class)
public class TreeBackedTypeElementTest extends CompilerTreeApiParameterizedTest {

  @Test
  public void testGetSimpleName() throws IOException {
    compile("public class Foo {}");
    TypeElement element = elements.getTypeElement("Foo");

    assertNameEquals("Foo", element.getSimpleName());
  }

  @Test
  public void testGetQualifiedNameUnnamedPackage() throws IOException {
    compile("public class Foo {}");

    TypeElement element = elements.getTypeElement("Foo");

    assertNameEquals("Foo", element.getQualifiedName());
  }

  @Test
  public void testGetQualifiedNameNamedPackage() throws IOException {
    compile(Joiner.on('\n').join(
        "package com.facebook.buck;",
        "public class Foo {}"));

    TypeElement element = elements.getTypeElement("com.facebook.buck.Foo");

    assertNameEquals("com.facebook.buck.Foo", element.getQualifiedName());
  }

  @Test
  public void testGetKindAnnotation() throws IOException {
    compile("@interface Foo { }");

    assertSame(ElementKind.ANNOTATION_TYPE, elements.getTypeElement("Foo").getKind());
  }

  @Test
  public void testGetKindClass() throws IOException {
    compile("class Foo { }");

    assertSame(ElementKind.CLASS, elements.getTypeElement("Foo").getKind());
  }

  @Test
  public void testGetKindEnum() throws IOException {
    compile("enum Foo {  }");

    assertSame(ElementKind.ENUM, elements.getTypeElement("Foo").getKind());
  }

  @Test
  public void testGetKindInterface() throws IOException {
    compile("interface Foo { }");

    assertSame(ElementKind.INTERFACE, elements.getTypeElement("Foo").getKind());
  }

  @Test
  public void testAccept() throws IOException {
    compile("class Foo { }");

    TypeElement expectedType = elements.getTypeElement("Foo");
    Object expectedResult = new Object();
    Object actualResult = expectedType.accept(new SimpleElementVisitor8<Object, Object>() {
      @Override
      protected Object defaultAction(Element e, Object o) {
        return null;
      }

      @Override
      public Object visitType(TypeElement actualType, Object o) {
        assertSame(expectedType, actualType);
        return o;
      }
    }, expectedResult);

    assertSame(expectedResult, actualResult);
  }

  @Test
  public void testToString() throws IOException {
    compile(Joiner.on('\n').join(
        "package com.facebook.buck;",
        "public class Foo<T> {}"));

    TypeElement element = elements.getTypeElement("com.facebook.buck.Foo");

    assertEquals("com.facebook.buck.Foo", element.toString());
  }

  @Test
  public void testGetQualifiedNameInnerClass() throws IOException {
    compile(Joiner.on('\n').join(
        "package com.facebook.buck;",
        "public class Foo {",
        "  public class Bar { }",
        "}"));

    TypeElement element = elements.getTypeElement("com.facebook.buck.Foo.Bar");

    assertNameEquals("com.facebook.buck.Foo.Bar", element.getQualifiedName());
  }

  @Test
  public void testGetQualifiedNameEnumAnonymousMember() throws IOException {
    compile(Joiner.on('\n').join(
        "public enum Foo {",
        "  BAR,",
        "  BAZ() {",
        "  }",
        "}"
    ));

    // Expect no crash. Enum anonymous members don't have qualified names, but at one point during
    // development we would crash trying to make one for them.
  }

  @Test
  public void testGetNestingKindTopLevel() throws IOException {
    compile("public class Foo { }");

    assertSame(NestingKind.TOP_LEVEL, elements.getTypeElement("Foo").getNestingKind());
  }

  @Test
  public void testGetNestingKindMember() throws IOException {
    compile(Joiner.on('\n').join(
        "public class Foo {",
        "  class Bar { }",
        "}"));

    assertSame(NestingKind.MEMBER, elements.getTypeElement("Foo.Bar").getNestingKind());
  }

  @Test
  public void testAsType() throws IOException {
    compile("class Foo { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    TypeMirror fooTypeMirror = fooElement.asType();

    assertEquals(TypeKind.DECLARED, fooTypeMirror.getKind());
    DeclaredType fooDeclaredType = (DeclaredType) fooTypeMirror;
    assertSame(fooElement, fooDeclaredType.asElement());
    assertEquals(0, fooDeclaredType.getTypeArguments().size());

    TypeMirror enclosingType = fooDeclaredType.getEnclosingType();
    assertEquals(TypeKind.NONE, enclosingType.getKind());
    assertTrue(enclosingType instanceof NoType);
  }

  @Test
  public void testGetSuperclassNoSuperclassIsObject() throws IOException {
    compile("class Foo { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    DeclaredType superclass = (DeclaredType) fooElement.getSuperclass();
    TypeElement objectElement = elements.getTypeElement("java.lang.Object");

    assertSame(objectElement, superclass.asElement());
  }

  @Test
  public void testGetSuperclassObjectSuperclassIsObject() throws IOException {
    compile("class Foo extends java.lang.Object { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    DeclaredType superclass = (DeclaredType) fooElement.getSuperclass();
    TypeElement objectElement = elements.getTypeElement("java.lang.Object");

    assertSame(objectElement, superclass.asElement());
  }

  @Test
  public void testGetSuperclassOfInterfaceIsNoneType() throws IOException {
    compile("interface Foo extends java.lang.Runnable { }");

    TypeElement fooElement = elements.getTypeElement("Foo");

    assertSame(TypeKind.NONE, fooElement.getSuperclass().getKind());
  }

  @Test
  public void testGetSuperclassOtherSuperclass() throws IOException {
    compile(ImmutableMap.of(
        "Foo.java",
        Joiner.on('\n').join(
            "package com.facebook.foo;",
            "public class Foo extends com.facebook.bar.Bar { }"
        ),
        "Bar.java",
        Joiner.on('\n').join(
            "package com.facebook.bar;",
            "public class Bar { }"
        )
    ));

    TypeElement fooElement = elements.getTypeElement("com.facebook.foo.Foo");
    TypeElement barElement = elements.getTypeElement("com.facebook.bar.Bar");

    DeclaredType superclass = (DeclaredType) fooElement.getSuperclass();
    assertSame(barElement, superclass.asElement());
  }

  @Test
  public void testGetParameterizedSuperclass() throws IOException {
    compile(Joiner.on('\n').join(
        "abstract class Foo extends java.util.ArrayList<java.lang.String> { }",
        "abstract class Bar extends java.util.ArrayList<java.lang.String> { }"));

    TypeElement listElement = elements.getTypeElement("java.util.ArrayList");
    TypeElement stringElement = elements.getTypeElement("java.lang.String");
    DeclaredType expectedSuperclass = types.getDeclaredType(
        listElement,
        stringElement.asType());

    TypeElement fooElement = elements.getTypeElement("Foo");
    TypeElement barElement = elements.getTypeElement("Bar");
    DeclaredType fooSuperclass = (DeclaredType) fooElement.getSuperclass();
    DeclaredType barSuperclass = (DeclaredType) barElement.getSuperclass();

    assertNotSame(expectedSuperclass, fooSuperclass);
    assertSameType(expectedSuperclass, fooSuperclass);
    assertSameType(fooSuperclass, barSuperclass);
  }

  @Test
  public void testGetReallyParameterizedSuperclass() throws IOException {
    compile(Joiner.on('\n').join(
        "abstract class Foo",
        "    extends java.util.HashMap<",
        "        java.util.ArrayList<java.lang.String>, ",
        "        java.util.HashSet<java.lang.Integer>> { }"));

    TypeElement mapElement = elements.getTypeElement("java.util.HashMap");
    TypeElement listElement = elements.getTypeElement("java.util.ArrayList");
    TypeElement setElement = elements.getTypeElement("java.util.HashSet");
    TypeMirror stringType = elements.getTypeElement("java.lang.String").asType();
    TypeMirror integerType = elements.getTypeElement("java.lang.Integer").asType();
    TypeMirror listStringType = types.getDeclaredType(listElement, stringType);
    TypeMirror setIntType = types.getDeclaredType(setElement, integerType);
    TypeMirror crazyMapType = types.getDeclaredType(mapElement, listStringType, setIntType);

    TypeElement fooElement = elements.getTypeElement("Foo");

    assertSameType(crazyMapType, fooElement.getSuperclass());
  }

  @Test
  public void testGetArrayParameterizedSuperclass() throws IOException {
    compile("abstract class Foo extends java.util.ArrayList<java.lang.String[]> { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    TypeElement listElement = elements.getTypeElement("java.util.ArrayList");
    TypeMirror stringType = elements.getTypeElement("java.lang.String").asType();
    ArrayType stringArrayType = types.getArrayType(stringType);

    DeclaredType expectedSuperclass = types.getDeclaredType(listElement, stringArrayType);

    assertSameType(expectedSuperclass, fooElement.getSuperclass());
  }

  @Test
  public void testGetMultiDimArrayParameterizedSuperclass() throws IOException {
    compile("abstract class Foo extends java.util.ArrayList<java.lang.String[][]> { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    TypeElement listElement = elements.getTypeElement("java.util.ArrayList");
    TypeMirror stringType = elements.getTypeElement("java.lang.String").asType();
    ArrayType stringArrayType = types.getArrayType(stringType);
    ArrayType stringArrayArrayType = types.getArrayType(stringArrayType);

    DeclaredType expectedSuperclass = types.getDeclaredType(listElement, stringArrayArrayType);

    assertSameType(expectedSuperclass, fooElement.getSuperclass());
  }

  @Test
  public void testGetPrimitiveArrayParameterizedSuperclass() throws IOException {
    compile("abstract class Foo extends java.util.ArrayList<int[]> { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    TypeElement listElement = elements.getTypeElement("java.util.ArrayList");
    TypeMirror intType = types.getPrimitiveType(TypeKind.INT);
    ArrayType intArrayType = types.getArrayType(intType);

    DeclaredType expectedSuperclass = types.getDeclaredType(listElement, intArrayType);

    assertSameType(expectedSuperclass, fooElement.getSuperclass());
  }

  @Test
  public void testGetTypevarParameterizedSuperclass() throws IOException {
    compile("abstract class Foo<T extends java.lang.Runnable> extends java.util.ArrayList<T> { }");

    TypeElement fooElement = elements.getTypeElement("Foo");

    TypeElement listElement = elements.getTypeElement("java.util.ArrayList");
    TypeMirror tType = fooElement.getTypeParameters().get(0).asType();

    DeclaredType expectedSuperclass = types.getDeclaredType(listElement, tType);
    assertSameType(expectedSuperclass, fooElement.getSuperclass());
  }

  @Test
  public void testEnclosedClasses() throws IOException {
    compile(Joiner.on('\n').join(
        "class Foo {",
        "  class Bar { }",
        "}"
    ));

    TypeElement fooElement = elements.getTypeElement("Foo");
    TypeElement barElement = elements.getTypeElement("Foo.Bar");

    List<Element> enclosedElements = new ArrayList<>(fooElement.getEnclosedElements());
    assertThat(enclosedElements, Matchers.hasItems(barElement));
    assertSame(fooElement, barElement.getEnclosingElement());
  }
}
