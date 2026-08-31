//! Version-string comparison shared by the CVE matcher, the package
//! collectors, and the client's outdated-package views.

use std::cmp::Ordering;

/// Compare two version strings the way pacman's vercmp does: alternating
/// numeric and alphabetic segments, numeric compared as numbers, numeric
/// beating alphabetic, and a trailing alphabetic segment counting as older
/// than nothing (`1.0rc1` < `1.0`).
pub(crate) fn vercmp(a: &str, b: &str) -> Ordering {
    let sa = segments(a);
    let sb = segments(b);
    let mut i = 0;
    loop {
        match (sa.get(i), sb.get(i)) {
            (None, None) => return Ordering::Equal,
            (None, Some(Segment::Alpha(_))) => return Ordering::Greater,
            (None, Some(_)) => return Ordering::Less,
            (Some(Segment::Alpha(_)), None) => return Ordering::Less,
            (Some(_), None) => return Ordering::Greater,
            (Some(x), Some(y)) => match x.cmp(y) {
                Ordering::Equal => i += 1,
                other => return other,
            },
        }
    }
}

/// One run of digits or letters. Numeric segments order after alphabetic ones,
/// matching pacman ("1.0a" < "1.0.1").
#[derive(PartialEq, Eq, PartialOrd, Ord)]
enum Segment<'a> {
    Alpha(&'a str),
    // Leading zeros stripped; longer strings are bigger, so (len, digits)
    // orders numerically without parsing (and without overflow).
    Num(usize, &'a str),
}

fn segments(version: &str) -> Vec<Segment<'_>> {
    let mut segments = Vec::new();
    let mut rest = version;
    while let Some(start) = rest.find(|c: char| c.is_ascii_alphanumeric()) {
        rest = &rest[start..];
        let end = if rest.starts_with(|c: char| c.is_ascii_digit()) {
            rest.find(|c: char| !c.is_ascii_digit())
                .unwrap_or(rest.len())
        } else {
            rest.find(|c: char| !c.is_ascii_alphabetic())
                .unwrap_or(rest.len())
        };
        let (segment, tail) = rest.split_at(end);
        segments.push(if segment.starts_with(|c: char| c.is_ascii_digit()) {
            let digits = segment.trim_start_matches('0');
            Segment::Num(digits.len(), digits)
        } else {
            Segment::Alpha(segment)
        });
        rest = tail;
    }
    segments
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn vercmp_orders_versions() {
        use Ordering::*;
        for (a, b, expected) in [
            ("1.0", "1.0", Equal),
            ("1.0.1", "1.0", Greater),
            ("1.0", "1.0.1", Less),
            ("1.10", "1.9", Greater),
            ("2.4.57", "2.4.57", Equal),
            // A trailing alphabetic segment is a pre-release.
            ("1.0rc1", "1.0", Less),
            ("1.0", "1.0rc1", Greater),
            // A numeric segment beats an alphabetic one.
            ("1.0.1", "1.0a", Greater),
            // Leading zeros don't matter.
            ("1.01", "1.1", Equal),
            ("3.0.13", "3.0.14", Less),
        ] {
            assert_eq!(vercmp(a, b), expected, "{a} vs {b}");
        }
    }
}
