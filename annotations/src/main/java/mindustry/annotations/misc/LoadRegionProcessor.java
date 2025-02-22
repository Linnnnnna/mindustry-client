package mindustry.annotations.misc;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.Load")
public class LoadRegionProcessor extends BaseProcessor{

    @Override
    public void process(RoundEnvironment env) throws Exception{
        TypeSpec.Builder regionClass = TypeSpec.classBuilder("ContentRegions")
            .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"deprecation\"").build())
            .addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder method = MethodSpec.methodBuilder("loadRegions")
            .addParameter(tname("mindustry.ctype.MappableContent"), "content")
            .addModifiers(Modifier.STATIC, Modifier.PUBLIC);

        OrderedMap<Stype, Seq<Svar>> fieldMap = new OrderedMap<>();

        for(Svar field : fields(Load.class)){
            if(!field.is(Modifier.PUBLIC)){
                err("@LoadRegion field must be public", field);
            }

            fieldMap.get(field.enclosingType(), Seq::new).add(field);
        }

        Seq<Stype> entries = Seq.with(fieldMap.keys());
        entries.sortComparing(e -> e.name());

        for(Stype type : entries){
            Seq<Svar> fields = fieldMap.get(type);
            fields.sortComparing(s -> s.name());
            method.beginControlFlow("if(content instanceof $L)", type.fullName());

            for(Svar field : fields){
                Load an = field.annotation(Load.class);
                //get # of array dimensions
                int dims = count(field.mirror().toString(), "[]");
                boolean doFallback = !an.fallback().equals("error");
                String fallbackString = doFallback ? ", " + parse(an.fallback()) : "";

                //not an array
                if(dims == 0){
                    method.addStatement("(($L)content).$L = $T.atlas.find($L$L)", type.fullName(), field.name(), Core.class, parse(an.value()), fallbackString);
                }else{
                    //is an array, create length string
                    int[] lengths = an.lengths();
                    if(lengths.length == 0) lengths = new int[]{an.length()};

                    if(dims != lengths.length){
                        err("Length dimensions must match array dimensions: " + dims + " != " + lengths.length, field);
                    }

                    StringBuilder lengthString = new StringBuilder();
                    for(int value : lengths) lengthString.append("[").append(value).append("]");

                    method.addStatement("(($T)content).$L = new $T$L", type.tname(), field.name(), TextureRegion.class, lengthString.toString());

                    for(int i = 0; i < dims; i++){
                        method.beginControlFlow("for(int INDEX$L = 0; INDEX$L < $L; INDEX$L ++)", i, i, lengths[i], i);
                    }

                    StringBuilder indexString = new StringBuilder();
                    for(int i = 0; i < dims; i++){
                        indexString.append("[INDEX").append(i).append("]");
                    }

                    method.addStatement("(($T)content).$L$L = $T.atlas.find($L$L)", type.tname(), field.name(), indexString.toString(), Core.class, parse(an.value()), fallbackString);

                    for(int i = 0; i < dims; i++){
                        method.endControlFlow();
                    }
                }
            }

            method.endControlFlow();
        }

        regionClass.addMethod(method.build());

        write(regionClass);
    }

    private static int count(String str, String substring){
        int lastIndex = 0;
        int count = 0;

        while(lastIndex != -1){

            lastIndex = str.indexOf(substring, lastIndex);

            if(lastIndex != -1){
                count ++;
                lastIndex += substring.length();
            }
        }
        return count;
    }

    private String parse(String value){
        value = '"' + value + '"';
        value = value.replace("@size", "\" + ((mindustry.world.Block)content).size + \"");
        value = value.replace("@", "\" + content.name + \"");
        value = value.replace("#1", "\" + INDEX0 + \"");
        value = value.replace("#2", "\" + INDEX1 + \"");
        value = value.replace("#", "\" + INDEX0 + \"");
        return value;
    }

}
