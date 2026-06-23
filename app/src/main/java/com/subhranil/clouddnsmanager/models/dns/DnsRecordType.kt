package com.subhranil.clouddnsmanager.models.dns

import kotlinx.serialization.Serializable

@Serializable
enum class DnsRecordType {
    A, AAAA, CAA, CERT, CNAME, DNSKEY, DS,
    HTTPS, LOC, MX, NAPTR, NS, PTR, SMIMEA,
    SOA, SPF, SRV, SSHFP, SVCB, TLSA, TXT, URI,
}