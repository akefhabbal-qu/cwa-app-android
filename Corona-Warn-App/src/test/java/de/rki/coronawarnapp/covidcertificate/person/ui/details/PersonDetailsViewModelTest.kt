package de.rki.coronawarnapp.covidcertificate.person.ui.details

import de.rki.coronawarnapp.covidcertificate.common.certificate.CertificatePersonIdentifier
import de.rki.coronawarnapp.covidcertificate.common.certificate.CwaCovidCertificate.State
import de.rki.coronawarnapp.covidcertificate.common.certificate.DccV1
import de.rki.coronawarnapp.covidcertificate.common.certificate.RecoveryDccV1
import de.rki.coronawarnapp.covidcertificate.common.certificate.TestDccV1
import de.rki.coronawarnapp.covidcertificate.common.certificate.VaccinationDccV1
import de.rki.coronawarnapp.covidcertificate.common.repository.RecoveryCertificateContainerId
import de.rki.coronawarnapp.covidcertificate.common.repository.TestCertificateContainerId
import de.rki.coronawarnapp.covidcertificate.common.repository.VaccinationCertificateContainerId
import de.rki.coronawarnapp.covidcertificate.person.core.PersonCertificates
import de.rki.coronawarnapp.covidcertificate.person.core.PersonCertificatesProvider
import de.rki.coronawarnapp.covidcertificate.person.ui.details.items.CwaUserCard
import de.rki.coronawarnapp.covidcertificate.person.ui.details.items.PersonDetailsQrCard
import de.rki.coronawarnapp.covidcertificate.person.ui.details.items.RecoveryCertificateCard
import de.rki.coronawarnapp.covidcertificate.person.ui.details.items.TestCertificateCard
import de.rki.coronawarnapp.covidcertificate.person.ui.details.items.VaccinationCertificateCard
import de.rki.coronawarnapp.covidcertificate.person.ui.details.items.VaccinationInfoCard
import de.rki.coronawarnapp.covidcertificate.person.ui.overview.PersonColorShade
import de.rki.coronawarnapp.covidcertificate.recovery.core.RecoveryCertificate
import de.rki.coronawarnapp.covidcertificate.test.core.TestCertificate
import de.rki.coronawarnapp.covidcertificate.vaccination.core.VaccinatedPerson
import de.rki.coronawarnapp.covidcertificate.vaccination.core.VaccinationCertificate
import de.rki.coronawarnapp.covidcertificate.vaccination.core.repository.VaccinationRepository
import de.rki.coronawarnapp.covidcertificate.validation.core.DccValidationRepository
import de.rki.coronawarnapp.util.TimeAndDateExtensions.toLocalDateUserTz
import de.rki.coronawarnapp.util.TimeStamper
import de.rki.coronawarnapp.util.qrcode.coil.CoilQrCode
import de.rki.coronawarnapp.util.ui.observeOnce
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScope
import org.joda.time.Instant
import org.joda.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import testhelpers.BaseTest
import testhelpers.TestDispatcherProvider
import testhelpers.extensions.InstantExecutorExtension
import testhelpers.extensions.getOrAwaitValue

@ExtendWith(InstantExecutorExtension::class)
class PersonDetailsViewModelTest : BaseTest() {
    @MockK lateinit var personCertificatesProvider: PersonCertificatesProvider
    @MockK lateinit var vaccinationRepository: VaccinationRepository
    @MockK lateinit var dccValidationRepository: DccValidationRepository
    @MockK lateinit var timeStamper: TimeStamper
    @MockK lateinit var viewModel: PersonDetailsViewModel
    private val vcContainerId = VaccinationCertificateContainerId("1")
    private val tcsContainerId = TestCertificateContainerId("2")
    private val rcContainerId = RecoveryCertificateContainerId("3")

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, true)
    }

    @Test
    fun `Navigates back when no person found`() {
        val vaccCert1 = mockVaccinationCertificate(1)
        val vaccCert2 = mockVaccinationCertificate(2)
        every { personCertificatesProvider.personCertificates } returns flowOf(
            setOf(
                PersonCertificates(
                    listOf(
                        mockRecoveryCertificate(),
                        mockTestCertificate(),
                        vaccCert1,
                        vaccCert2
                    )
                )
            )
        )

        personDetailsViewModel("personIdentifierCode").apply {
            uiState.observeOnce { } // To trigger the flow
            events.getOrAwaitValue() shouldBe Back
        }
    }

    @Test
    fun `Ui state and navigation`() {
        val vaccCert1 = mockVaccinationCertificate(1)
        val vaccCert2 = mockVaccinationCertificate(2)
        every { personCertificatesProvider.personCertificates } returns flowOf(
            setOf(
                PersonCertificates(
                    listOf(
                        mockRecoveryCertificate(),
                        mockTestCertificate(),
                        vaccCert1,
                        vaccCert2
                    )
                )
            )
        )

        coEvery { personCertificatesProvider.setCurrentCwaUser(any()) } just Runs

        every { timeStamper.nowUTC } returns Instant.EPOCH
        val vaccinatedPerson = mockk<VaccinatedPerson>().apply {
            every { vaccinationCertificates } returns setOf(vaccCert1, vaccCert2)
            every { getVaccinationStatus(any()) } returns VaccinatedPerson.Status.IMMUNITY
            every { getDaysUntilImmunity(any()) } returns null
            every { getDaysSinceLastVaccination() } returns 21
            every { boosterRule } returns null
            every { identifier } returns certificatePersonIdentifier
            every { hasBoosterNotification } returns false
        }
        every { vaccinationRepository.vaccinationInfos } returns flowOf(setOf(vaccinatedPerson))
        personDetailsViewModel(certificatePersonIdentifier.codeSHA256)
            .apply {
                uiState.getOrAwaitValue().also {

                    it.name shouldBe vaccCert1.fullName

                    it.certificateItems.run {
                        get(0) as PersonDetailsQrCard.Item

                        get(1) as VaccinationInfoCard.Item

                        (get(2) as CwaUserCard.Item).apply {
                            onSwitch(true)
                            coVerify { personCertificatesProvider.setCurrentCwaUser(any()) }
                        }
                        (get(3) as RecoveryCertificateCard.Item).apply {
                            onClick()
                            events.getOrAwaitValue() shouldBe OpenRecoveryCertificateDetails(rcContainerId)
                        }

                        (get(4) as TestCertificateCard.Item).apply {
                            onClick()
                            events.getOrAwaitValue() shouldBe OpenTestCertificateDetails(tcsContainerId)
                        }

                        (get(5) as VaccinationCertificateCard.Item).apply {
                            onClick()
                            events.getOrAwaitValue() shouldBe OpenVaccinationCertificateDetails(vcContainerId)
                        }

                        (get(6) as VaccinationCertificateCard.Item).apply {
                            onClick()
                            events.getOrAwaitValue() shouldBe OpenVaccinationCertificateDetails(vcContainerId)
                        }
                    }
                }
            }
    }

    private fun personDetailsViewModel(personCode: String) = PersonDetailsViewModel(
        dispatcherProvider = TestDispatcherProvider(),
        vaccinationRepository = vaccinationRepository,
        dccValidationRepository = dccValidationRepository,
        timeStamper = timeStamper,
        personCertificatesProvider = personCertificatesProvider,
        personIdentifierCode = personCode,
        colorShade = PersonColorShade.COLOR_1,
        appScope = TestCoroutineScope()
    )

    private fun mockTestCertificate(): TestCertificate = mockk<TestCertificate>().apply {
        every { certificateId } returns "testCertificateId"
        every { fullName } returns "Andrea Schneider"
        every { rawCertificate } returns mockk<TestDccV1>().apply {
            every { test } returns mockk<DccV1.TestCertificateData>().apply {
                every { testType } returns "LP6464-4"
                every { sampleCollectedAt } returns Instant.parse("2021-05-31T11:35:00.000Z")
            }
        }
        every { headerExpiresAt } returns Instant.parse("2022-01-01T11:35:00.000Z")
        every { containerId } returns tcsContainerId
        every { testType } returns "PCR-Test"
        every { dateOfBirthFormatted } returns "18.04.1943"
        every { sampleCollectedAt } returns Instant.parse("2021-05-31T11:35:00.000Z")
        every { registeredAt } returns Instant.parse("2021-05-21T11:35:00.000Z")
        every { personIdentifier } returns certificatePersonIdentifier
        every { isValid } returns true
        every { getState() } returns State.Valid(headerExpiresAt)
        every { qrCodeToDisplay } returns CoilQrCode("qrCode")
    }

    private fun mockVaccinationCertificate(number: Int = 1, final: Boolean = false): VaccinationCertificate =
        mockk<VaccinationCertificate>().apply {
            val localDate = Instant.parse("2021-06-01T11:35:00.000Z").toLocalDateUserTz()
            every { certificateId } returns "vaccinationCertificateId$number"
            every { fullName } returns "Andrea Schneider"
            every { rawCertificate } returns mockk<VaccinationDccV1>().apply {
                every { vaccination } returns mockk<DccV1.VaccinationData>().apply {
                    every { doseNumber } returns number
                    every { totalSeriesOfDoses } returns 2
                    every { vaccinatedOn } returns localDate
                    every { medicalProductId } returns "EU/1/20/1528"
                }
            }
            every { containerId } returns vcContainerId
            every { vaccinatedOn } returns localDate
            every { personIdentifier } returns certificatePersonIdentifier
            every { medicalProductName } returns "EU/1/20/1528"
            every { doseNumber } returns number
            every { totalSeriesOfDoses } returns 2
            every { isSeriesCompletingShot } returns final
            every { isValid } returns true
            every { getState() } returns State.Valid(expiresAt = Instant.parse("2022-01-01T11:35:00.000Z"))
            every { qrCodeToDisplay } returns CoilQrCode("qrCode")
        }

    private fun mockRecoveryCertificate(): RecoveryCertificate =
        mockk<RecoveryCertificate>().apply {
            every { certificateId } returns "recoveryCertificateId"
            every { validUntil } returns Instant.parse("2021-05-31T11:35:00.000Z").toLocalDateUserTz()
            every { personIdentifier } returns certificatePersonIdentifier
            every { qrCodeToDisplay } returns CoilQrCode("qrCode")
            every { containerId } returns rcContainerId
            every { fullName } returns "Andrea Schneider"
            every { isValid } returns true
            every { rawCertificate } returns mockk<RecoveryDccV1>().apply {
                every { recovery } returns mockk<DccV1.RecoveryCertificateData>().apply {
                    every { validFrom } returns LocalDate.now()
                }
            }
            every { getState() } returns State.Valid(expiresAt = Instant.parse("2022-01-01T11:35:00.000Z"))
        }

    private val certificatePersonIdentifier = CertificatePersonIdentifier(
        dateOfBirthFormatted = "01.01.2020",
        firstNameStandardized = "firstNameStandardized",
        lastNameStandardized = "lastNameStandardized",
    )
}
