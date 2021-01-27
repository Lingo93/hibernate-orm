/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.immutable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alex Burgel
 */
@TestForIssue( jiraKey = "HHH-11330" )
@RequiresDialectFeature( value = DialectChecks.SupportsIdentityColumns.class )
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" )
		}
)
@DomainModel( annotatedClasses = IdentityGeneratorWithNaturalIdCacheTest.Person.class )
@SessionFactory
public class IdentityGeneratorWithNaturalIdCacheTest {
	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Person person = new Person();
					person.setName( "John Doe" );
					session.persist( person );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete Person" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10659")
	public void testNaturalIdCacheEntry(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		assertThat( statistics.getSecondLevelCacheHitCount(), Matchers.is( 0L ) );
		assertThat( statistics.getNaturalIdCacheHitCount(), Matchers.is( 0L ) );

		scope.inTransaction(
				(session) -> {
					session.bySimpleNaturalId( Person.class ).load( "John Doe" );
					assertThat( statistics.getSecondLevelCacheHitCount(), Matchers.is( 0L ) );
					assertThat( statistics.getNaturalIdCacheHitCount(), Matchers.is( 1L ) );
				}
		);

		scope.inTransaction(
				(session) -> {
					session.bySimpleNaturalId( Person.class ).load( "John Doe" );
					assertThat( statistics.getSecondLevelCacheHitCount(), Matchers.is( 1L ) );
					assertThat( statistics.getNaturalIdCacheHitCount(), Matchers.is( 2L ) );
				}
		);
	}

	@Entity(name = "Person")
	@NaturalIdCache
	@Cache( usage = CacheConcurrencyStrategy.READ_ONLY )
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@NaturalId
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
