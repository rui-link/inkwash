/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.base.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import tools.jackson.databind.annotation.JsonSerialize;

import top.ruilink.inkwash.base.util.DataMaskSerializer;

/**
 * Annotation marking fields for sensitive value masking.
 *
 * <p>
 * Masking is applied at serialisation time. By default the value is **always**
 * masked.
 *
 * <p>
 * Set {@link #exemptForSelf()} to {@code true} on a field that identifies the
 * record's owner (typically the {@code id} of a user-shaped view). Only then
 * will the serializer compare that id with the current principal and emit the
 * plaintext for self-views.
 *
 * <p>
 * This used to be implicit: the serializer reflected on any {@code getId()} and
 * compared it with the current user, so every view that happened to expose a
 * {@code Long getId()} silently became exempt. The opt-in is explicit so that
 * adding a field can never accidentally unmask it.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskSerializer.class)
@Documented
public @interface DataMask {

	/**
	 * Masking rule to apply.
	 *
	 * <p>
	 * Declared first so {@code @DataMask(MaskType.NAME)} keeps working as the
	 * positional shorthand; appending new elements after it is safe.
	 *
	 * @return the masking rule
	 */
	MaskType value() default MaskType.PHONE;

	/**
	 * Whether the owning principal may see this field unmasked.
	 *
	 * <p>
	 * Requires the annotated field to be the record owner's id (see
	 * {@link top.ruilink.inkwash.base.util.DataMaskSerializer}). When {@code false}
	 * — the default — the value is masked for everyone including the owner.
	 *
	 * @return {@code true} to exempt self-views
	 */
	boolean exemptForSelf() default false;

	enum MaskType {
		PHONE, EMAIL, ID_CARD, NAME, ADDRESS, CUSTOM
	}

	String maskChar() default "*";

	int prefixLen() default 3;

	int suffixLen() default 4;
}
